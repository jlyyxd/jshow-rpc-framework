package github.jlyyxd.ServiceImpl;

import github.jlyyxd.Data;
import github.jlyyxd.MyService;

//@RpcService(group = "test", version = "1.0")
public class MyServiceImpl implements MyService {
    @Override
    public String hello(Data data) {
        return "调用MyServiceImpl处理请求:"+data.toString();
    }
}
