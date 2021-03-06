package cn.aethli.thoth.service;

import cn.aethli.thoth.common.enums.LotteryType;
import cn.aethli.thoth.common.exception.RetryException;
import cn.aethli.thoth.common.utils.TermUtils;
import cn.aethli.thoth.entity.CWLData;
import cn.aethli.thoth.entity.CWLResult;
import cn.aethli.thoth.entity.Lottery;
import cn.aethli.thoth.entity.MData;
import cn.aethli.thoth.entity.PELottery;
import cn.aethli.thoth.feign.CWLLotteryFeign;
import cn.aethli.thoth.feign.PELotteryFeign;
import cn.aethli.thoth.repository.CWLResultRepository;
import cn.aethli.thoth.repository.PELotteryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.io.IOException;
import java.util.Iterator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

/**
 * @author Termite
 */
@Slf4j
@Service
@EnableAsync
public class DataGetTaskServiceImpl implements DataGetTaskService {

  private ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private PELotteryFeign peLotteryFeign;
  @Autowired private PELotteryRepository peLotteryRepository;
  @Autowired private CWLLotteryFeign cwlLotteryFeign;
  @Autowired private CWLResultRepository cwlResultRepository;
  @Autowired private SpiderService spiderService;

  @Async
  @Override
  public void getPELotteries(String type, String startTerm, String num, String endTerm)
      throws IOException {
    while (Integer.parseInt(startTerm) <= Integer.parseInt(endTerm)) {
      log.info(String.format("start with term=%s", startTerm));
      String lottery = peLotteryFeign.getLottery(type, TermUtils.termParamsConvert(startTerm), num);
      JsonNode jsonNode = objectMapper.readTree(lottery);
      jsonNode = jsonNode.findParent("mdata");
      Iterator<JsonNode> mdataJsonNodes;
      try {
        mdataJsonNodes = jsonNode.findValue("mdata").elements();
      } catch (NullPointerException e) {
        log.info(String.format("can not parse json data,term=%s", startTerm));
        startTerm = TermUtils.peTermJump(type, startTerm, num);
        continue;
      }
      MData thisMData;
      while (mdataJsonNodes.hasNext()) {
        thisMData = objectMapper.treeToValue(mdataJsonNodes.next(), MData.class);
        try {
          peLotteryRepository.save(thisMData.getLottery());
        } catch (Exception e) {
          log.info(String.format("can not insert,term=%s", startTerm));
        }
      }
      startTerm = TermUtils.peTermJump(type, startTerm, num);
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    log.info(String.format("task complete,startTerm=%sendTerm=%s", startTerm, endTerm));
  }

  @Async
  @Override
  public void getCWLLotteries(String name, String issueStart, String issueEnd, String issueCount) {
    while (Integer.parseInt(issueStart) + 1 < Integer.parseInt(issueEnd)) {
      String lottery = null;
      try {
        String end =
            String.valueOf(
                Integer.parseInt(issueStart)
                    + Integer.parseInt(issueCount == null ? "1" : issueCount));
        lottery =
            cwlLotteryFeign.getLottery("fakeBrowser", name, null, issueStart, end, null, null);
      } catch (FeignException e) {
        e.printStackTrace();
      }
      try {
        CWLData cwlData = objectMapper.readValue(lottery, CWLData.class);
        for (CWLResult cwlResult : cwlData.getResult()) {
          cwlResult
              .getPrizeGrades()
              .forEach(cwlPrizeGrade -> cwlPrizeGrade.setCwlResult(cwlResult));
          try {
            cwlResultRepository.save(cwlResult);
          } catch (Exception e) {
            log.info(String.format("can not insert,issue=%s", issueStart));
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        issueStart = TermUtils.cwlIssueJump(name, issueStart, issueCount);
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  @Async
  @Override
  public void getCom500Data(String type, String startTerm, String endTerm) throws RetryException {
    int term = Integer.parseInt(startTerm);
    while (term < Integer.parseInt(endTerm)) {
      Lottery com500Data = spiderService.getCom500Data(type, term);
      term = Integer.parseInt(TermUtils.cwlIssueJump(type, String.valueOf(term), "1"));
      if (com500Data == null) {
        continue;
      } else if (com500Data instanceof CWLResult) {
        CWLResult cwlResult = (CWLResult) com500Data;
        try {
          cwlResultRepository.save(cwlResult);
        } catch (DataIntegrityViolationException e) {
          log.info(String.format("can not insert,issue=%s", String.valueOf(term)));
        }
      } else if (com500Data instanceof PELottery) {
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public cn.aethli.thoth.dto.Lottery getPELotteryThisTerm(LotteryType type) {
    String lottery = peLotteryFeign.getLottery(type.getParam(), "1");
    JsonNode jsonNode = null;
    try {
      jsonNode = objectMapper.readTree(lottery);
      jsonNode = jsonNode.findParent("mdata");
      Iterator<JsonNode> mdataJsonNodes;
      mdataJsonNodes = jsonNode.findValue("mdata").elements();
      MData thisMData;
      if (mdataJsonNodes.hasNext()) {
        thisMData = objectMapper.treeToValue(mdataJsonNodes.next(), MData.class);
        if (thisMData != null) {
          cn.aethli.thoth.dto.Lottery result = new cn.aethli.thoth.dto.Lottery();
          result.setDate(thisMData.getLottery().getOpenTime());
          result.setTerm(thisMData.getLottery().getTerm());
          result.setType(LotteryType.get(String.valueOf(thisMData.getLottery().getLType())));
          return result;
        }
      }
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }
}
