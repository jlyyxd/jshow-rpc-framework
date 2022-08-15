package github.jlyyxd;

import lombok.AllArgsConstructor;

import java.io.Serializable;

@lombok.Data
@AllArgsConstructor
public class Data implements Serializable {
    String name;
    Integer age;
}
