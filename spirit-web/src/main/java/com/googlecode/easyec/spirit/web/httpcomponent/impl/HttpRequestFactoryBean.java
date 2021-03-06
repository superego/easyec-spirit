package com.googlecode.easyec.spirit.web.httpcomponent.impl;

import com.googlecode.easyec.spirit.web.httpcomponent.HttpRequest;
import com.googlecode.easyec.spirit.web.httpcomponent.HttpRequestFactory;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.util.Assert;

/**
 * HTTP请求工厂类。此类用于初始化一个默认的HTTP请求对象。
 *
 * @author JunJie
 * @see HttpRequestFactory
 */
public class HttpRequestFactoryBean implements SmartFactoryBean<HttpRequest>, InitializingBean {

    private HttpClient httpClient;

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public boolean isPrototype() {
        return true;
    }

    @Override
    public boolean isEagerInit() {
        return false;
    }

    @Override
    public HttpRequest getObject() throws Exception {
        return new InternalHttpRequestImpl(httpClient);
    }

    @Override
    public Class<?> getObjectType() {
        return HttpRequest.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(httpClient);
    }
}
