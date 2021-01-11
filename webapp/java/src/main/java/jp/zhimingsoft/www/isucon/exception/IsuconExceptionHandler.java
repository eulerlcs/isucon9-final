package jp.zhimingsoft.www.isucon.exception;


import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.ZonedDateTime;

@RestControllerAdvice
public class IsuconExceptionHandler extends ResponseEntityExceptionHandler {
    // Controller から throw される IsuconException を捕捉
    @ExceptionHandler(IsuconException.class)
    public ResponseEntity<Object> handleMyException(IsuconException exception, WebRequest request) {
        HttpHeaders headers = new HttpHeaders();


        return super.handleExceptionInternal(exception,
                createErrorResponseBody(exception, request),
                headers,
                exception.getHttpStatus(),
                request);
    }

    // レスポンスのボディ部を作成
    private IsuconExceptionResponseBody createErrorResponseBody(IsuconException exception, WebRequest request) {
        IsuconExceptionResponseBody isuconExceptionResponseBody = new IsuconExceptionResponseBody();

        String responseErrorMessage = exception.getHttpStatus().getReasonPhrase();
        String uri = ((ServletWebRequest) request).getRequest().getRequestURI();

        isuconExceptionResponseBody.setStatus(exception.getHttpStatus().value());
        isuconExceptionResponseBody.setError(exception.isError());
        isuconExceptionResponseBody.setMessage(exception.getMessage());

        isuconExceptionResponseBody.setExceptionOccurrenceTime(ZonedDateTime.now());
        isuconExceptionResponseBody.setErrorMessage(responseErrorMessage);
        isuconExceptionResponseBody.setPath(uri);

        return isuconExceptionResponseBody;
    }
}