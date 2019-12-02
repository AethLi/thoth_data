package cn.aethli.thoth.cache;

import cn.aethli.thoth.common.enums.LotteryType;
import cn.aethli.thoth.dto.Lottery;
import cn.aethli.thoth.repository.CWLResultRepository;
import cn.aethli.thoth.repository.PELotteryRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;

/**
 * @author Termite
 * @device Hades
 * @date 2019-12-02 16:36
 */
@CacheConfig(cacheNames = "lottery")
public class LotteryCacheImpl implements LotteryCache {

  @Autowired private CWLResultRepository cwlResultRepository;
  @Autowired private PELotteryRepository peLotteryRepository;

  @CachePut(cacheNames = "lottery", key = "type.value")
  @Override
  public List<Lottery> getLottery(LotteryType type) {
    switch (type) {
      case QXC:
        peLotteryRepository.findByType(type);
    }
    return null;
  }
}