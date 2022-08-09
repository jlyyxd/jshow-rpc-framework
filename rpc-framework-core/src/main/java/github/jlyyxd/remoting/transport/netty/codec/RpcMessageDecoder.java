package github.jlyyxd.remoting.transport.netty.codec;

import github.jlyyxd.enums.CompressTypeEnum;
import github.jlyyxd.enums.SerializationTypeEnum;
import github.jlyyxd.extension.ExtensionLoader;
import github.jlyyxd.compress.Compress;
import github.jlyyxd.remoting.constants.RpcConstants;
import github.jlyyxd.remoting.dto.RpcMessage;
import github.jlyyxd.remoting.dto.RpcRequest;
import github.jlyyxd.remoting.dto.RpcResponse;
import github.jlyyxd.serialze.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * <p>
 * custom protocol decoder
 * 将二进制流解码成RpcMessage
 * <p>
 * 协议：消息头部16B，消息体 (full length - 16)B
 * <pre>
 *   0    1    2    3    4        5     6    7    8    9          10    11      12    13   14  15   16
 *   +----+----+----+----+--------+----+----+----+----+-----------+------+----- --+----+----+----+----+
 *   |   magic   code    |version |    full length    |messageType| codec|compress|    RequestId      |
 *   +-------------------+--------+-------------------+-----------+------+--------+-------------------+
 *   |                                                                                                |
 *   |                                         body                                                   |
 *   |                                                                                                |
 *   |                                        ... ...                                                 |
 *   +------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B codec（序列化类型）     1B compress（压缩类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 * </pre>
 *
 * @see <a href="https://zhuanlan.zhihu.com/p/95621344">LengthFieldBasedFrameDecoder解码器</a>
 */
@Slf4j
public class RpcMessageDecoder extends LengthFieldBasedFrameDecoder {
    public RpcMessageDecoder() {
        // lengthFieldOffset: magic code is 4B, and version is 1B, and then full length. so value is 5
        // lengthFieldLength: full length is 4B. so value is 4
        // lengthAdjustment: full length include all data and read 9 bytes before, so the left length is (fullLength-9). so values is -9
        // initialBytesToStrip: we will check magic code and version manually, so do not strip any bytes. so values is 0
        this(RpcConstants.MAX_FRAME_LENGTH, 5, 4, -9, 0);
    }

    /**
     * @param maxFrameLength      Maximum frame length. It decide the maximum length of data that can be received.
     *                            If it exceeds, the data will be discarded.
     * @param lengthFieldOffset   Length field offset. The length field is the one that skips the specified length of byte.
     * @param lengthFieldLength   The number of bytes in the length field.
     * @param lengthAdjustment    The compensation value to add to the value of the length field
     * @param initialBytesToStrip Number of bytes skipped.
     *                            If you need to receive all of the header+body data, this value is 0
     *                            if you only want to receive the body data, then you need to skip the number of bytes consumed by the header.
     */
    public RpcMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                             int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame.readableBytes() >= RpcConstants.TOTAL_LENGTH) {
            // 存在消息体
            try {
                return decodeFrame(frame);
            } catch (Exception e) {
                log.error("Decode frame error!", e);
                throw e;
            } finally {
                frame.release();
            }
        }
        return frame;
    }

    private Object decodeFrame(ByteBuf in) {
        // 检查魔数
        checkMagicNumber(in);
        // 版本号
        checkVersion(in);
        // 帧总长度 4B
        int fullLength = in.readInt();
        // 消息类型 1B
        byte messageType = in.readByte();
        // 序列化类型 1B
        byte codecType = in.readByte();
        // 压缩类型 1B
        byte compressType = in.readByte();
        // 请求Id 4B
        int requestId = in.readInt();
        final RpcMessage rpcMessage = RpcMessage.builder()
                .messageType(messageType)
                .codec(codecType)
                .compress(compressType)
                .requestId(requestId).build();

        // 心跳包请求
        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            rpcMessage.setData(RpcConstants.PING);
            return rpcMessage;
        }
        // 心跳包响应
        if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            rpcMessage.setData(RpcConstants.PONG);
            return rpcMessage;
        }
        // 消息体长度
        int bodyLength = fullLength - RpcConstants.HEAD_LENGTH;
        if (bodyLength > 0) {
            byte[] body = new byte[bodyLength];
            in.readBytes(body);
            // 解压缩
            String compressName = CompressTypeEnum.getName(compressType);
            Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
            body = compress.decompress(body);
            // 反序列化
            String codecName = SerializationTypeEnum.getName(codecType);
            log.info("code name: [{}] ", codecName);
            Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
            // 请求和相应的反序列化方式不同
            if (messageType == RpcConstants.REQUEST_TYPE) {
                RpcRequest data = serializer.deserialize(body, RpcRequest.class);
                rpcMessage.setData(data);
            } else {
                RpcResponse<?> data = serializer.deserialize(body, RpcResponse.class);
                rpcMessage.setData(data);
            }
        }
        return rpcMessage;
    }

    // 检查帧首部的版本号是否对应
    private void checkVersion(ByteBuf in) {
        final byte version = in.readByte();
        if (version != RpcConstants.VERSION) {
            throw new RuntimeException("version isn't compatible");
        }
    }

    // 检查帧首部的魔数是否对应
    private void checkMagicNumber(ByteBuf in) {
        int len = RpcConstants.MAGIC_NUMBER.length;
        final byte[] temp = new byte[len];
        in.readBytes(temp);
        for (int i = 0; i < len; i++) {
            if (temp[i] != RpcConstants.MAGIC_NUMBER[i]) {
                throw new IllegalArgumentException("Unkown magic code:" + Arrays.toString(temp));
            }
        }
    }
}
