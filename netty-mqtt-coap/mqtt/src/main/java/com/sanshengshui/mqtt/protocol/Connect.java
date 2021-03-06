package com.sanshengshui.mqtt.protocol;

import com.sanshengshui.mqtt.common.auth.GrozaAuthService;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @author 穆书伟
 * @description CONNECT连接处理
 */
public class Connect {
    public static final Logger LOGGER = LoggerFactory.getLogger(Connect.class);

    private GrozaAuthService grozaAuthService;

    public Connect(GrozaAuthService grozaAuthService){
        this.grozaAuthService = grozaAuthService;
    }
    //消息解码器出现异常
    public void processConnect(Channel channel, MqttConnectMessage msg){
        if (msg.decoderResult().isFailure()){
            Throwable cause = msg.decoderResult().cause();
            if (cause instanceof MqttUnacceptableProtocolVersionException){
                //不支持的协议版本
                MqttConnAckMessage connAckMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.CONNACK,false,MqttQoS.AT_MOST_ONCE,false,0),
                        new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION,false),
                        null
                );
                channel.writeAndFlush(connAckMessage);
                channel.close();
                return;
            }else if (cause instanceof MqttIdentifierRejectedException){
                //不合格的clientId
                MqttConnAckMessage connAckMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.CONNACK,false,MqttQoS.AT_MOST_ONCE,false,0),
                        new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED,false),
                        null
                );
                channel.writeAndFlush(connAckMessage);
                channel.close();
                return;
            }
        }
        //clientId为空或null的情况，这里要求必须提供clientId,不管cleanSession是否为1,此处没有参考标准协议实现
        if (StringUtils.isEmpty(msg.payload().clientIdentifier())){
            MqttConnAckMessage connAckMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                    new MqttFixedHeader(MqttMessageType.CONNACK,false,MqttQoS.AT_LEAST_ONCE,false,0),
                    new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED,false),
                    null
            );
            channel.writeAndFlush(connAckMessage);
            channel.close();
            return;
        }
        // 用户名和密码验证, 这里要求客户端连接时必须提供用户名和密码, 不管是否设置用户名标志和密码标志为1, 此处没有参考标准协议实现
        String username = msg.payload().userName();
        //这里可以用加密算法对密码进行加密
        String password = msg.payload().passwordInBytes() == null ? null : new String(msg.payload().passwordInBytes(), CharsetUtil.UTF_8);
        if (!grozaAuthService.checkValid(username,password)){
            MqttConnAckMessage connAckMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                    new MqttFixedHeader(MqttMessageType.CONNACK,false,MqttQoS.AT_MOST_ONCE,false,0),
                    new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD,false),
                    null
            );
            channel.writeAndFlush(connAckMessage);
            channel.close();
            return;
        }

    }
}
