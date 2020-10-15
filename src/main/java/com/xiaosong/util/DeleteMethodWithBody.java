package com.xiaosong.util;

import org.apache.commons.httpclient.methods.EntityEnclosingMethod;

public class DeleteMethodWithBody extends EntityEnclosingMethod {

    @Override
    public String getName() {
        return "DELETE";
    }

    public DeleteMethodWithBody() {
    }

    public DeleteMethodWithBody(String uri) {
        super(uri);
    }
}
