package com.salesmanager.shop.store.services;

import java.util.HashMap;

public class BaseApiController {

    protected HashMap<String, Object> getMeta(int errorCode, int httpCode, String message) {
        HashMap<String, Object> meta = new HashMap<>();

        meta.put("error_code", errorCode);
        meta.put("http_code", httpCode);
        meta.put("message", message);

        return meta;
    }

    protected HashMap<String, Object> getErrorResponse(HashMap meta) {
        HashMap<String, Object> errorResponse = new HashMap<>();

        errorResponse.put("meta", meta);
        errorResponse.put("data", new HashMap<>());

        return errorResponse;
    }
}
