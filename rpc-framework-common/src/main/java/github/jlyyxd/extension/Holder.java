package github.jlyyxd.extension;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Holder <T>{
    private volatile T value;
}
