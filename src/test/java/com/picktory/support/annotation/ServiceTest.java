package com.picktory.support.annotation;

import com.picktory.support.DatabaseCleanerExtension;
import com.picktory.support.config.TestConfig;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = TestConfig.class)
@ExtendWith(DatabaseCleanerExtension.class)
public @interface ServiceTest {
}