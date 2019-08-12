package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;
import org.tron.common.storage.Deposit;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.db.AccountStore;
import org.tron.core.db.DelegationStore;
import org.tron.core.db.DynamicPropertiesStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.protos.Protocol.Vote;

@Slf4j
@Component
public class DelegationService {

  @Setter
  private Manager manager;

  public void payStandbyWitness() {
    List<ByteString> witnessAddressList = new ArrayList<>();
    for (WitnessCapsule witnessCapsule : manager.getWitnessStore().getAllWitnesses()) {
      witnessAddressList.add(witnessCapsule.getAddress());
    }
    sortWitness(witnessAddressList);
    if (witnessAddressList.size() > ChainConstant.MAX_ACTIVE_WITNESS_NUM) {
      witnessAddressList = witnessAddressList.subList(0, ChainConstant.MAX_ACTIVE_WITNESS_NUM);
    }

    long voteSum = 0;
    long totalPay = 115_200_000_000L / 7200;
    for (ByteString b : witnessAddressList) {
      voteSum += getWitnesseByAddress(b).getVoteCount();
    }
    long cycle = manager.getDynamicPropertiesStore().getCurrentCycleNumber();
    if (voteSum > 0) {
      for (ByteString b : witnessAddressList) {
        long pay = (long) (getWitnesseByAddress(b).getVoteCount() * ((double) totalPay / voteSum));
        manager.getDelegationStore().addReward(cycle, b.toByteArray(), pay);
      }
    }

  }

  public void payBlockReward(byte[] witnessAddress, long value) {
    long cycle = manager.getDynamicPropertiesStore().getCurrentCycleNumber();
    manager.getDelegationStore().addReward(cycle, witnessAddress, value);
  }

  public void withdrawReward(byte[] address, Deposit deposit) {
    if (!manager.getDynamicPropertiesStore().allowChangeDelegation()) {
      return;
    }
    AccountStore accountStore = manager.getAccountStore();
    DelegationStore delegationStore = manager.getDelegationStore();
    DynamicPropertiesStore dynamicPropertiesStore = manager.getDynamicPropertiesStore();
    AccountCapsule accountCapsule;
    if (deposit == null) {
      accountCapsule = accountStore.get(address);
    } else {
      accountCapsule = deposit.getAccount(address);
    }
    long beginCycle = delegationStore.getBeginCycle(address);
    long endCycle = delegationStore.getEndCycle(address);
    long reward = 0;
    //withdraw the latest cycle reward
    if (beginCycle + 1 == endCycle) {
      AccountCapsule account = delegationStore.getAccountVote(beginCycle, address);
      if (account != null) {
        reward = computeReward(beginCycle, account);
      }
      beginCycle += 1;
    }
    //
    endCycle = dynamicPropertiesStore.getCurrentCycleNumber();
    if (accountCapsule == null || CollectionUtils.isEmpty(accountCapsule.getVotesList())) {
      manager.getDelegationStore().setBeginCycle(address,
          dynamicPropertiesStore.getCurrentCycleNumber());
      return;
    }
    if (beginCycle < endCycle) {
      for (long cycle = beginCycle; cycle < endCycle; cycle++) {
        reward += computeReward(cycle, accountCapsule);
      }
      try {
        manager.adjustAllowance(address, reward);
      } catch (BalanceInsufficientException e) {
        logger.error("withdrawReward error: {},{}", Hex.toHexString(address), reward, e);
      }
      delegationStore.setBeginCycle(address, endCycle);
      delegationStore.setEndCycle(address, endCycle + 1);
      delegationStore.setAccountVote(endCycle, address, accountCapsule);
    }
  }

  public void queryReward(byte[] address) {

  }

  private long computeReward(long cycle, AccountCapsule accountCapsule) {
    long reward = 0;
    for (Vote vote : accountCapsule.getVotesList()) {
      long totalReward = manager.getDelegationStore()
          .getReward(cycle, vote.getVoteAddress().toByteArray());
      long totalVote = manager.getDelegationStore()
          .getWitnessVote(cycle, vote.getVoteAddress().toByteArray());
      if (totalVote == DelegationStore.REMARK) {
        totalVote = manager.getWitnessStore().get(vote.getVoteAddress().toByteArray())
            .getVoteCount();
      }
      long userVote = vote.getVoteCount();
      reward += (long) ((double) (userVote / totalVote) * totalReward);
    }
    return reward;
  }

  public WitnessCapsule getWitnesseByAddress(ByteString address) {
    return this.manager.getWitnessStore().get(address.toByteArray());
  }

  private long getEndCycle(byte[] address) {
    long endCycle = manager.getDelegationStore().getEndCycle(address);
    if (endCycle == DelegationStore.REMARK) {
      endCycle = manager.getDynamicPropertiesStore().getCurrentCycleNumber();
    }
    return endCycle;
  }

  private void sortWitness(List<ByteString> list) {
    list.sort(Comparator.comparingLong((ByteString b) -> getWitnesseByAddress(b).getVoteCount())
        .reversed()
        .thenComparing(Comparator.comparingInt(ByteString::hashCode).reversed()));
  }

}
