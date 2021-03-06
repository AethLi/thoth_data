package cn.aethli.thoth.config;

import cn.aethli.lunar.exception.LunarException;
import cn.aethli.thoth.common.enums.LotteryType;
import cn.aethli.thoth.common.enums.VersionType;
import cn.aethli.thoth.common.exception.LotteryException;
import cn.aethli.thoth.common.utils.LotteryUtils;
import cn.aethli.thoth.dto.Lottery;
import cn.aethli.thoth.entity.DataVersion;
import cn.aethli.thoth.entity.PELottery;
import cn.aethli.thoth.repository.DataVersionRepository;
import cn.aethli.thoth.repository.PELotteryRepository;
import cn.aethli.thoth.service.DataGetTaskService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

/** @author Termite */
@Order(1)
@EnableAsync
@Component
@Slf4j
public class DataInitializationRunner implements CommandLineRunner {

  @Autowired private DataGetTaskService dataGetTaskService;
  @Autowired private PELotteryRepository peLotteryRepository;
  @Autowired private DataVersionRepository dataVersionRepository;

  @Override
  public void run(String... args) {
    peDataInitialization();
    cwlDataInitialization();
  }

  @Async
  void peDataInitialization() {
    // 七星彩
    Lottery lastLottery = dataGetTaskService.getPELotteryThisTerm(LotteryType.QXC);
    DataVersion version =
        dataVersionRepository.findByType(VersionType.QXC_UPDATE).orElseGet(DataVersion::new);
    version.setType(VersionType.QXC_UPDATE);
    version.setVersion(VersionType.QXC_UPDATE.getDesc());
    Optional<PELottery> peLotteryOptional =
        peLotteryRepository.findTopByOpenTimeAndLType(Integer.valueOf(LotteryType.QXC.getParam()));
    if (peLotteryOptional.isPresent()) {
      LocalDate openTime = peLotteryOptional.get().getOpenTime();
      if (openTime.isAfter(version.getUpdateDt())) {
        String offlineTerm = "0";
        try {
          offlineTerm = LotteryUtils.date2Term(LocalDate.now(), LotteryType.QXC);
        } catch (LotteryException | LunarException e) {
          log.error(e.getMessage(), e);
          // todo 异常处理
          System.exit(0);
        }
        if (Integer.parseInt(offlineTerm) < Integer.parseInt(peLotteryOptional.get().getTerm())) {

        }
      }
    }else {

    }
    try {
      dataGetTaskService.getPELotteries(
          LotteryType.QXC.getParam(), "4000", "50", lastLottery.getTerm());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void cwlDataInitialization() {}
}
