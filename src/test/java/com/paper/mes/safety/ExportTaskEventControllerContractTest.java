package com.paper.mes.safety;

import com.paper.mes.exporttask.controller.ExportTaskEventController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ExportTaskEventControllerContractTest {

    @Test
    void eventController_whenClientDisconnects_handlesIOExceptionLocally() throws NoSuchMethodException {
        Method handler = ExportTaskEventController.class.getMethod("handleClientDisconnect");
        ExceptionHandler annotation = handler.getAnnotation(ExceptionHandler.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactly(IOException.class);
        assertThat(handler.getReturnType()).isEqualTo(void.class);
    }
}
