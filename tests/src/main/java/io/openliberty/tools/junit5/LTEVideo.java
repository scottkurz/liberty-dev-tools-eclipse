package io.openliberty.tools.junit5;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by sergey on 12.02.17.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.ANNOTATION_TYPE})
@ExtendWith(LTEVideoExtension.class)
@com.automation.remarks.video.annotations.Video
public @interface LTEVideo {
  String name() default "";
}
