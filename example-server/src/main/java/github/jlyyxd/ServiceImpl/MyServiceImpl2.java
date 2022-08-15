package github.jlyyxd.ServiceImpl;

import github.jlyyxd.Data;
import github.jlyyxd.MyService;
import github.jlyyxd.annotation.RpcService;

@RpcService(group = "test", version = "2.0")
public class MyServiceImpl2 implements MyService {
    @Override
    public String hello(Data data) {
        return "调用MyServiceImpl2处理请求:"+data.toString();
    }
}
