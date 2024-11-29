package cn.wolfcode.domain;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;


@Setter@Getter
public class LoginLog implements Serializable {  //实现Serializable接口让这个类可以序列化，存到redis时会自动序列化，用rocketmq也需要序列化，可以使用Jackson代替java自带的序列化，更加高效
    public static Boolean LOGIN_SUCCESS = Boolean.TRUE;
    public static Boolean LOGIN_FAIL = Boolean.FALSE;
    public LoginLog(){
        super();
    }
    public LoginLog(Long phone,String loginIp,Date loginTime){
        this.phone = phone;
        this.loginIp = loginIp;
        this.loginTime = loginTime;
    }
    private Long id;//自增id
    private Long phone;//登陆用户的手机号码
    private String loginIp;//登录IP
    private Date loginTime;///登录时间
    private Boolean state = LOGIN_SUCCESS;//登录状态
}
