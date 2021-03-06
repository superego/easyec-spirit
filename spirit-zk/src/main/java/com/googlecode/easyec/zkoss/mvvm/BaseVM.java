package com.googlecode.easyec.zkoss.mvvm;

import com.googlecode.easyec.zkoss.ui.Breadcrumb;
import com.googlecode.easyec.zkoss.ui.BreadcrumbCtrl;
import com.googlecode.easyec.zkoss.ui.Steps;
import com.googlecode.easyec.zkoss.ui.builders.DefaultUiParameterBuilder;
import com.googlecode.easyec.zkoss.ui.builders.UiBuilder;
import com.googlecode.easyec.zkoss.ui.builders.UriUiParameterBuilder;
import com.googlecode.easyec.zkoss.ui.events.BreadcrumbEvent;
import com.googlecode.easyec.zkoss.ui.events.StepOutEvent;
import com.googlecode.easyec.zkoss.ui.listeners.StepOutEventListener;
import com.googlecode.easyec.zkoss.ui.listeners.StepOutUpperEventListener;
import com.googlecode.easyec.zkoss.ui.pushstate.DefaultPopState;
import com.googlecode.easyec.zkoss.ui.pushstate.PushState;
import com.googlecode.easyec.zkoss.utils.ExecUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.zkoss.bind.BindComposer;
import org.zkoss.bind.Binder;
import org.zkoss.bind.annotation.AfterCompose;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.Init;
import org.zkoss.web.util.resource.ServletRequestResolver;
import org.zkoss.xel.VariableResolver;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.metainfo.PageDefinition;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.ComponentActivationListener;
import org.zkoss.zkplus.spring.DelegatingVariableResolver;
import org.zkoss.zul.Messagebox;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.googlecode.easyec.zkoss.mvvm.BaseVM.FindScope.*;
import static com.googlecode.easyec.zkoss.ui.Breadcrumb.TYPE_PAGE;
import static com.googlecode.easyec.zkoss.ui.Breadcrumb.TYPE_URI;
import static com.googlecode.easyec.zkoss.ui.builders.ExecutionUiBuilderImpl.PARAM_PAGE_DEF;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.zkoss.bind.annotation.ContextType.COMPONENT;
import static org.zkoss.bind.sys.BinderCtrl.VM;

/**
 * 模型视图-视图模型的基础类。
 * 此类封装了 {@link Messagebox}的一些方法。
 *
 * @author JunJie
 */
public abstract class BaseVM<T extends Component> implements ComponentActivationListener, Serializable, Steps, BreadcrumbCtrl {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /* 标识是否做过激活操作 */
    private transient boolean _activated;
    /* 面包屑对象 */
    private Breadcrumb _breadcrumb;
    /* 当前ZK组件对象 */
    private T self;

    @WireVariable
    private transient UiBuilder uiBuilder;

    /**
     * 返回当前注入的ZK UI构建器对象实例
     *
     * @return <code>UiBuilder</code>
     */
    public UiBuilder getUiBuilder() {
        return uiBuilder;
    }

    /**
     * 返回当前组件的引用对象
     *
     * @return <code>Component</code>子类
     */
    public T getSelf() {
        return self;
    }

    @Init
    public void initVM(@ContextParam(COMPONENT) T comp) {
        this.self = comp;
        // 注入全局变量
        wireVariables(comp, false);

        logger.trace("initVM() done!");

        doInit();
    }

    @AfterCompose
    public void afterInit() {
        // 注入组件对象
        wireComponents(self, false);
        // 注入组件对象的事件
        wireEventListener(self);

        logger.trace("afterInit() done!");

        doAfterCompose();
    }

    /**
     * 为给定的ZK组件注入变量的方法
     *
     * @param comp   ZK组件对象
     * @param rewire 表示是否执行重新注入的方法
     */
    protected void wireVariables(Component comp, boolean rewire) {
        if (rewire) {
            Selectors.rewireVariablesOnActivate(
                comp, this, createVariableResolvers()
            );
        } else {
            Selectors.wireVariables(
                comp, this, createVariableResolvers()
            );
        }
    }

    /**
     * 为给定的ZK组件注入相应组件的方法
     *
     * @param comp   ZK组件对象
     * @param rewire 表示是否执行重新注入的方法
     */
    protected void wireComponents(Component comp, boolean rewire) {
        if (rewire) {
            Selectors.rewireComponentsOnActivate(comp, this);
        } else {
            Selectors.wireComponents(comp, this, false);
        }
    }

    /**
     * 为给定的ZK组件注入相应组件事件的方法
     *
     * @param comp ZK组件对象
     */
    protected void wireEventListener(Component comp) {
        Selectors.wireEventListeners(comp, this);
    }

    public void didActivate(Component comp) {
        if (!_activated && self.equals(comp)) {
            wireVariables(comp, true);
            wireComponents(comp, true);
            _activated = true;
        }
    }

    public void willPassivate(Component comp) {
        _activated = false;
    }

    /**
     * 获取当前已设置的
     * <code>{@link Binder}</code>
     * 对象实例
     *
     * @return <code>Binder</code>对象的实现
     */
    protected Binder getBinder() {
        return (Binder) getSelf().getAttribute(
            (String) getSelf().getAttribute(
                BindComposer.BINDER_ID
            )
        );
    }

    /**
     * 执行初始化操作的方法
     *
     * @see Init
     */
    protected void doInit() { /* no op */ }

    /**
     * 执行初始化后置操作的方法
     *
     * @see AfterCompose
     */
    protected void doAfterCompose() {
        /* 从外部获取面包屑信息 */
        Breadcrumb _bc = findParameter(BC_ID, Arg, Breadcrumb.class);
        if (_bc == null) _bc = findParameter(BC_ID, Execution, Breadcrumb.class);

        // 如果外部没有提供面包屑，则试图构造面包屑
        if (_bc == null) {
            HttpServletRequest request = ExecUtils.getNativeRequest();
            String uri = request.getRequestURI();
            String base = request.getContextPath();
            if (StringUtils.isNotBlank(base)) {
                uri = uri.replaceFirst(base, "");
            }

            /*
             * 如果是以/zkau开头，则说明是ZK的ajax
             * 请求创建的页面，那么就获取当前页面的
             * 绝对路径
             */
            if (uri.startsWith("/zkau")) {
                PageDefinition _def = findParameter(PARAM_PAGE_DEF, All, PageDefinition.class);
                if (_def != null) this._breadcrumb = createBreadcrumb(_def.getRequestPath(), TYPE_PAGE);
            } else this._breadcrumb = createBreadcrumb(uri, TYPE_URI);
        } else this._breadcrumb = _bc;
    }

    /**
     * 创建一组<code>VariableResolver</code>对象列表
     *
     * @return ZK变量解析对象列表
     */
    protected List<VariableResolver> createVariableResolvers() {
        return Arrays.asList(
            new com.googlecode.easyec.zkoss.DelegatingVariableResolver(),
            new DelegatingVariableResolver(),
            new ServletRequestResolver()
        );
    }

    /**
     * 查找参数值的方法，如果搜索范围提供的时All，
     * 那么该方法会按照下面的顺序来查找
     * 当前VM中设置的参数值。<br/>
     * <ol>
     * <li>当前<code>Execution</code>的Arg范围</li>
     * <li>当前<code>Execution</code>范围</li>
     * <li>当前<code>Session</code>范围</li>
     * </ol>
     *
     * @param key   参数KEY
     * @param scope <code>FindScope</code>
     * @param type  返回的对象类型
     * @param <R>   返回的对象类型枚举
     * @return 参数KEY对应的值
     */
    protected <R> R findParameter(String key, FindScope scope, Class<R> type) {
        if (isBlank(key) || scope == null || type == null) return null;

        R ret = null;
        switch (scope) {
            case Arg:
                ret = _cast(type, _findInArgs(key));
                break;
            case Execution:
                ret = _cast(type, _findInExec(key));
                break;
            case Session:
                ret = _cast(type, _findInSess(key));
                break;
            case All:
                ret = findParameter(key, Arg, type);
                if (ret != null) break;
                ret = findParameter(key, Execution, type);
                if (ret != null) break;
                ret = findParameter(key, Session, type);
        }

        return ret;
    }

    /**
     * 方法说明参见{@link #stepIn(DefaultUiParameterBuilder)}
     *
     * @param uri ZK page uri
     */
    public void stepIn(String uri) {
        stepIn(uri, Collections.emptyMap());
    }

    /**
     * 方法说明参见{@link #stepIn(DefaultUiParameterBuilder)}
     *
     * @param uri  ZK page uri
     * @param args 参数集合
     */
    public void stepIn(String uri, Map<Object, Object> args) {
        Assert.isTrue(isNotBlank(uri), "URI mustn't be null.");

        stepIn(
            UriUiParameterBuilder
                .create()
                .setUri(uri)
                .setParent(self.getParent())
                .setArgs(args)
        );
    }

    /**
     * 从当前的ZK组件页面步入给定的UI页面参数构造
     * 器中的方法。如果给定的ZK页面组件创建成功，则
     * 当前页面将从当前的父页面组件中脱离出来，
     * 并加载新构造的页面组件。当新构造的页面触发
     * 事件onStepOut时，当前的页面组件才会
     * 被恢复并重新展现出来。
     *
     * @param builder URI参数构造器对象
     */
    public void stepIn(DefaultUiParameterBuilder builder) {
        Assert.notNull(builder, "UiParameterBuilder object mustn't be null.");

        // 强制设置父组件为null
        builder.setParent(null);

        Component _newComp = uiBuilder.manufacture(builder.build());

        // 组件创建成功后
        if (_newComp != null) {
            StepOutEventListener lsnr = createStepOutEventListener();
            if (lsnr != null) {
                // 获取当前组件的父组件
                Component _parent = self.getParent();
                // 托管当前页面
                self.detach();
                // 移除当前页面的所有元素
                lsnr.removeChildren(_parent);
                // 设置新组件的父组件
                _newComp.setParent(_parent);
                // 添加onStepOut事件监听
                _newComp.addEventListener("onStepOut", lsnr);
                // 设置子组件的父级面包屑
                Object _vm = _newComp.getAttribute(VM);
                if (_vm != null && _vm instanceof BreadcrumbCtrl) {
                    Breadcrumb _bc = ((BreadcrumbCtrl) _vm).getBreadcrumb();
                    if (_bc != null) {
                        _bc.setParent(getBreadcrumb());
                        // 触发面包屑变更事件通知
                        Events.postEvent(new BreadcrumbEvent(_parent, _bc));
                    }
                }

                // 将当前组件状态推送至客户端
                PushState.push(new DefaultPopState(_newComp));
            }
        }
    }

    /**
     * 从当前ZK页面组件中步出，
     * 并返回到之前的页面的方法
     */
    public void stepOut() {
        stepOut(null);
    }

    /**
     * 从当前ZK页面组件中步出，
     * 并返回到之前的页面的方法
     *
     * @param data 返回到之前页面的数据对象
     */
    public void stepOut(Object data) {
        Events.postEvent(new StepOutEvent(self, data));
    }

    @Override
    public Breadcrumb getBreadcrumb() {
        return this._breadcrumb;
    }

    /**
     * 创建面包屑对象实例
     *
     * @param uri  页面URI
     * @param type 面包屑类型
     * @return 面包屑对象
     */
    protected Breadcrumb createBreadcrumb(String uri, int type) {
        return new Breadcrumb(getBreadcrumbLabel(), uri, type);
    }

    /**
     * 返回面包屑的显示文字
     *
     * @return 文字内容
     */
    protected String getBreadcrumbLabel() {
        return null;
    }

    /**
     * 创建一个<code>StepOutEventListener</code>对象实例的方法
     *
     * @return {@link StepOutEventListener}
     */
    protected StepOutEventListener createStepOutEventListener() {
        return new StepOutUpperEventListener(self, self.getParent());
    }

    private Object _findInArgs(String key) {
        Map<?, ?> args = Executions.getCurrent().getArg();
        return MapUtils.isNotEmpty(args) && args.containsKey(key)
            ? args.get(key) : null;
    }

    private Object _findInExec(String key) {
        return Executions.getCurrent().getAttribute(key);
    }

    private Object _findInSess(String key) {
        return Executions.getCurrent().getSession().getAttribute(key);
    }

    private <R> R _cast(Class<R> type, Object ret) {
        if (ret == null) return null;
        if (type.isInstance(ret)) return type.cast(ret);

        logger.warn("The parameter value [{}] returned isn't instanceof Class. [{}].",
            ret.getClass(), type);

        return null;
    }

    /**
     * 标识查询参数值范围的枚举类
     */
    public enum FindScope {

        /**
         * All scopes
         */
        All,
        /**
         * Execution.Arg
         */
        Arg,
        /**
         * Execution
         */
        Execution,
        /**
         * Session
         */
        Session
    }
}
