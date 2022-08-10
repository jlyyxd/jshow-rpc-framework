package github.jlyyxd;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@AllArgsConstructor
@Getter
@Slf4j
@ToString
public class Student implements Serializable {
    int age;
    String name;
    String gender;
}
