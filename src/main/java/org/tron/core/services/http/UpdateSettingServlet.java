package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.utils.TransactionUtil;
import org.tron.protos.Contract.UpdateSettingContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;


@Component
@Slf4j(topic = "API")
public class UpdateSettingServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String contract = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(contract);
      UpdateSettingContract.Builder build = UpdateSettingContract.newBuilder();
      JsonFormat.merge(contract, build);
      long delaySeconds = 0;
      JSONObject jsonObject = JSONObject.parseObject(contract);
      if (jsonObject.containsKey(Constant.DELAY_SECONDS)) {
        delaySeconds = jsonObject.getLong(Constant.DELAY_SECONDS);
      }

      Transaction tx;
      if (delaySeconds > 0) {
        tx = wallet.createDeferredTransactionCapsule(build.build(), delaySeconds, ContractType.UpdateSettingContract).getInstance();
        tx = TransactionUtil.setTransactionDelaySeconds(tx, delaySeconds);
      } else {
        tx = wallet
            .createTransactionCapsule(build.build(), ContractType.UpdateSettingContract)
            .getInstance();
      }

      response.getWriter().println(Util.printTransaction(tx));
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }
}
