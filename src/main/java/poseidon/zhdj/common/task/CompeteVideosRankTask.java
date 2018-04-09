package poseidon.zhdj.common.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import poseidon.zhdj.db.video.service.VideoService;

/**
 * 获取点赞、送礼前150名的定时任务
 * 
 * @author hulufeng
 * 
 */
@Component
public class CompeteVideosRankTask {
	private static Logger log = LoggerFactory.getLogger(CompeteVideosRankTask.class);
	@Autowired
	VideoService videoService;
	
	// 每天凌晨0点10执行一次
	@Scheduled(cron = "0 10 0 * * ?")
//	@Scheduled(cron = "0 0/1 * * * *")
	public void execute() {
		try {

		} catch (Exception e) {
			log.error("CompeteVideosRankTask", e);
		}
		
	}
}
