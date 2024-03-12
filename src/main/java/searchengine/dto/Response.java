package searchengine.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;

@RequiredArgsConstructor
@Data
public class Response {
    private boolean result;
    private String error;

}
