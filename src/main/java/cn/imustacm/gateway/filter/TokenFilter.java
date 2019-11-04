package cn.imustacm.gateway.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.exception.ZuulException;

public class TokenFilter extends ZuulFilter {

    /**
     * 过滤器类型
     */
    @Override
    public String filterType() {
        return "pre";
    }

    /**
     * 过滤器执行顺序
     * 数字越小 优先执行
     *
     * @return
     */
    @Override
    public int filterOrder() {
        return 0;
    }

    /**
     * 是否经过过滤器逻辑
     *
     * @return
     */
    @Override
    public boolean shouldFilter() {

        return false;
    }

    /**
     * 过滤器逻辑
     *
     * @return
     * @throws ZuulException
     */
    @Override
    public Object run() throws ZuulException {
        return null;
    }
}
