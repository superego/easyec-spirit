package com.googlecode.easyec.spirit.web.test.convertor;

import com.googlecode.easyec.spirit.web.controller.formbean.impl.SearchFormBean;
import com.googlecode.easyec.spirit.web.qseditors.CustomDateQsEditor;
import com.googlecode.easyec.spirit.web.qseditors.CustomNumberQsEditor;
import com.googlecode.easyec.spirit.web.qseditors.CustomStringQsEditor;
import com.googlecode.easyec.spirit.web.qseditors.QueryStringEditor;
import com.googlecode.easyec.spirit.web.test.httpcomponent.soap.User;
import com.googlecode.easyec.spirit.web.utils.WebUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.springframework.beans.propertyeditors.ClassEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;

import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Created by 俊杰 on 2014/5/2.
 */
public class PropertyEditorTest {

    @Test
    public void convertValue() {
        PropertyEditor editor = new CustomNumberEditor(Integer.class, true);
        editor.setAsText("2");
        Object v = editor.getValue();
        System.out.println(v);

        ClassEditor e = new ClassEditor();
        e.setAsText("java.lang.String");
        Object o = e.getValue();
        System.out.println(o);
    }

    @Test
    public void buildUrl() throws Exception {
        String s = URLEncoder.encode("name=1&value=-12323 2.0", "utf-8");
        System.out.println(s);

        s = Base64.encodeBase64String(s.getBytes("utf-8"));
        System.out.println(s);
    }

    @Test
    public void toHashCode() {
        System.out.println(HashCodeBuilder.reflectionHashCode("java.lang.String"));
        System.out.println(HashCodeBuilder.reflectionHashCode(String.class.getName()));
        System.out.println(HashCodeBuilder.reflectionHashCode(String.class));
    }

    @Test
    public void getSearchTermsAsText() {
        User user = new User();
        user.setName("JunJie");
        user.setAge(32);

        User user2 = new User();
        user2.setName("JunJie2");
        user.setAge(31);

        /* 初始化QueryStringEditor */
        Map<String, QueryStringEditor> editors = new HashMap<String, QueryStringEditor>();
        editors.put("a", new CustomNumberQsEditor(Integer.class));
        editors.put("b", new CustomNumberQsEditor(Float.class));
        editors.put("c", new CustomStringQsEditor());
        editors.put("d", new CustomNumberQsEditor(BigDecimal.class));
        editors.put("e", new CustomDateQsEditor());

        SearchFormBean bean = new SearchFormBean();
        bean.setEditors(editors);

        bean.addSearchTerm("a", 123);
        bean.addSearchTerm("b", 100.02f);
        bean.addSearchTerm("c", "My name*");
        bean.addSearchTerm("d", new BigDecimal("129.23"));
        bean.addSearchTerm("e", new Date());
        bean.addSearchTerm("f", user);
        bean.addSearchTerm("g", user);
        bean.addSearchTerm("h", user2);

        Map<String, String> map = bean.getSearchTermsAsText();
        Set<String> keySet = map.keySet();
        for (String key : keySet) {
            System.out.println(key + ": " + map.get(key));
        }

        String s = WebUtils.encodeQueryString(map);
        System.out.println(s);

        map = WebUtils.decodeQueryString(s);

        SearchFormBean bean1 = new SearchFormBean();
        bean1.setEditors(editors);
        bean1.setSearchTermsAsText(map);
        Map<String, Object> terms = bean1.getSearchTerms();
        Set<String> keys = terms.keySet();
        for (String key : keys) {
            Object o = terms.get(key);
            System.out.println(key + ": " + o + ", type: " + o.getClass().getName());
        }
    }

    @Test
    public void formatDate() throws ParseException {
        String s = DateFormatUtils.format(new Date(), "yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        System.out.println(s);

        Date d = DateUtils.parseDate(s, new String[] { "yyyy-MM-dd'T'HH:mm:ss.SSSZ" });
        System.out.println(d);
    }

    @Test
    public void getReferer() {
        String s = _getReferer("HTTP://tco.cx.tnt.com/site/cases/mycases.html?category=11");
        System.out.println(s);
    }

    private String _getReferer(String referer) {
        // 去掉URL参数
        int j = referer.indexOf("?");

        if (j > -1) referer = referer.substring(0, j);

        // 去掉HTTP或HTTPS
        referer = referer.toLowerCase().replaceAll("(http://|https://)", "");

        // 去掉域名、IP、端口信息
        int i = referer.indexOf("/");

        referer = referer.substring(i);

        // 去掉WEB应用的上下文
        String base = "/site";
        if (isNotBlank(base)) {
            referer = referer.replaceFirst(base, "");
        }

        return referer;
    }
}
