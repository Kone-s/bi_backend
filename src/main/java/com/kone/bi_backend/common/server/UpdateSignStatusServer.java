package com.kone.bi_backend.common.server;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.kone.bi_backend.common.utils.ThrowUtils;
import com.kone.bi_backend.common.response.ErrorCode;
import com.kone.bi_backend.model.entity.Score;
import com.kone.bi_backend.service.ScoreService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 更新用户签到状态
 */
@Component
public class UpdateSignStatusServer {

    @Resource
    private ScoreService scoreService;

    /**
     * 每天12点更新
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void updateSignStatus() {
        UpdateWrapper<Score> updateWrapper = new UpdateWrapper<>();
        //更新Score表中isSign为1的数据
        updateWrapper.set("is_sign", 0)
                .eq("is_sign", 1);
        boolean updateResult = scoreService.update(updateWrapper);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR);
    }
}
