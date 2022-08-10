package github.jlyyxd;

import github.jlyyxd.enums.CompressTypeEnum;
import github.jlyyxd.enums.SerializationTypeEnum;
import github.jlyyxd.remoting.constants.RpcConstants;
import github.jlyyxd.remoting.dto.RpcMessage;
import github.jlyyxd.remoting.dto.RpcRequest;
import github.jlyyxd.serialze.Serializer;
import github.jlyyxd.serialze.hessian.HessianSerializer;
import github.jlyyxd.serialze.kyro.KyroSerializer;
import github.jlyyxd.serialze.protostuff.ProtostuffSerializer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class TestSerializer {

    private RpcMessage rpcMessage;
    private RpcRequest rpcRequest;

    @Before
    public void buildRpcMessage() {
        Student jack = new Student(15,"jack","male");
        rpcRequest = RpcRequest.builder()
                .group("test")
                .version("version1")
                .requestId("1")
                .interfaceName("github.jlyyxd.MyService")
                .methodName("hello")
                .parameters(new Object[]{jack})
                .paramTypes(new Class[]{Student.class})
                .build();

        rpcMessage = RpcMessage.builder()
                .messageType(RpcConstants.REQUEST_TYPE)
                .compress(CompressTypeEnum.GZIP.getCode())
                .codec(SerializationTypeEnum.HESSIAN.getCode())
                .data(rpcRequest)
                .build();
    }

    // 测试成功
    @Test
    public void testHessianSerializer() {
        Student jack = new Student(15,"jack","male");
        Serializer serializer = new HessianSerializer();
        byte[] bytes = serializer.serialize(rpcMessage);
        RpcMessage result = serializer.deserialize(bytes, RpcMessage.class);
        System.out.println(result);
    }

    // 测试失败
    @Test
    public void testProtostuffSerializer() {
        Student jack = new Student(15,"jack","male");
        Serializer serializer = new ProtostuffSerializer();
        byte[] bytes = serializer.serialize(rpcRequest);
        RpcRequest result = serializer.deserialize(bytes, RpcRequest.class);
        System.out.println(result);
    }

    // 测试失败
    @Test
    public void testKryoSerializer() {
        Student jack = new Student(15,"jack","male");
        Integer str= 3;
        Serializer serializer = new KyroSerializer();
        byte[] bytes = serializer.serialize(jack);
        Student result = serializer.deserialize(bytes, Student.class);
        System.out.println(result);
    }
}
