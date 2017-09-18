package com.salesmanager.shop.store.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletResponse;
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

    protected void setResponse(HttpServletResponse response, HashMap responseMap) throws Exception{
        Gson gson = new GsonBuilder().create();
        String jsonString = gson.toJson(responseMap);
        IOUtils.write(jsonString.getBytes(), response.getOutputStream());// where 'resp' is your HttpServletResponse
        IOUtils.closeQuietly(response.getOutputStream());
    }
}
