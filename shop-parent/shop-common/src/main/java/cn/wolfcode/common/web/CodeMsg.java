package cn.wolfcode.common.web;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Created by wolfcode
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CodeMsg implements Serializable {
    private Integer code;
    private String msg;

    public static final CodeMsg ILLEGAL_OPERATION = new CodeMsg(500403, "非法操作");
}
