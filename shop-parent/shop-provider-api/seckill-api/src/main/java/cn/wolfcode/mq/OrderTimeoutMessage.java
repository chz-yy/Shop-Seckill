package cn.wolfcode.mq;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author xiaoliu
 * @date 2023/6/10
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimeoutMessage {

    private String orderNo;
    private Long seckillId;
    private Long userPhone;
}
