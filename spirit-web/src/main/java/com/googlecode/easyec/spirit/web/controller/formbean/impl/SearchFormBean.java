package com.googlecode.easyec.spirit.web.controller.formbean.impl;

import com.googlecode.easyec.spirit.web.controller.formbean.annotations.SearchTermType;
import com.googlecode.easyec.spirit.web.controller.formbean.terms.SearchTermsFilter;
import com.googlecode.easyec.spirit.web.controller.formbean.terms.SearchTermsTransform;
import com.googlecode.easyec.spirit.web.controller.sorts.Sort;
import com.googlecode.easyec.spirit.web.qseditors.QueryStringEditor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;
import static org.apache.commons.collections4.MapUtils.unmodifiableMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 表单搜索的Bean类。
 *
 * @author JunJie
 */
public class SearchFormBean extends AbstractSearchFormBean {

    private static final String PAGE_NAME = "pageNumber";
    private static final long serialVersionUID = -7311418367064648005L;

    /* 搜索条件过滤器类 */
    private List<SearchTermsFilter> filters = new ArrayList<SearchTermsFilter>();

    /* 搜索条件转换类 */
    private List<SearchTermsTransform> transforms = new ArrayList<SearchTermsTransform>();

    /* 查询参数转URL参数值的编辑器列表 */
    private Map<String, QueryStringEditor> editors = new LinkedHashMap<String, QueryStringEditor>();

    /* 搜索条件集合 */
    private Map<String, SearchValue> searchTerms = new LinkedHashMap<String, SearchValue>();

    /* 排序信息集合 */
    private List<Sort> sorts = new ArrayList<Sort>();

    /* 当前分页的页码 */
    private int currentPage = 1;

    public Map<String, Object> getSearchTerms() {
        Map<String, Object> terms = new HashMap<String, Object>();
        Set<String> keySet = searchTerms.keySet();
        for (String key : keySet) {
            terms.put(key, searchTerms.get(key).getValue());
        }

        return terms;
    }

    @Override
    public Map<String, Object> getRawSearchTerms() {
        Map<String, Object> terms = new HashMap<String, Object>();
        Set<String> keySet = searchTerms.keySet();
        for (String key : keySet) {
            terms.put(key, searchTerms.get(key).getRawValue());
        }

        return terms;
    }

    @Override
    public void setSearchTermsAsText(Map<String, String> params) {
        if (params != null && !params.isEmpty()) {
            Set<String> editorKeys = editors.keySet();
            for (String editorKey : editorKeys) {
                if (!params.containsKey(editorKey)) {
                    logger.warn("The editor's key isn't in URL parameter list, so skip it.");

                    continue;
                }

                try {
                    addSearchTerm(
                        editorKey,
                        editors.get(editorKey).coerceToBean(
                            URLDecoder.decode(params.get(editorKey), "utf-8")
                        )
                    );
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            // 设置分页页码
            if (params.containsKey(PAGE_NAME)) {
                try {
                    setPageNumber(Integer.parseInt(params.get(PAGE_NAME)));
                } catch (Exception e) { /* no op */ }
            }
        }
    }

    @Override
    public Map<String, String> getSearchTermsAsText() {
        if (editors.isEmpty()) {
            logger.warn("No any QueryStringEditor was added, so query string won't be transformed.");

            return emptyMap();
        }

        Map<String, String> map = new HashMap<String, String>();
        Set<String> editorKeys = editors.keySet();
        for (String editKey : editorKeys) {
            if (!searchTerms.containsKey(editKey)) {
                logger.warn("The editor's key isn't in current search term list, so skip it.");

                continue;
            }

            Object v = searchTerms.get(editKey).getRawValue();
            if (logger.isDebugEnabled()) {
                logger.debug("Original value is: [{}].", v);
            }

            QueryStringEditor editor = editors.get(editKey);
            if (!editor.accept(v)) {
                logger.debug("The value cannot be accepted by QueryStringEditor.");

                continue;
            }

            try {
                String s = editor.coerceToQs(v);
                logger.debug("The query string is: [{}].", s);

                if (isNotBlank(s)) map.put(editKey, URLEncoder.encode(s, "utf-8"));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        // 添加当前分页页码
        map.put(PAGE_NAME, String.valueOf(getPageNumber()));

        return map;
    }

    public void addSearchTerm(String name, Object value) {
        if (null == name || null == value) {
            logger.debug("Term's name or value either is null.");

            return;
        }

        boolean acceptTerm = true;
        for (SearchTermsFilter filter : filters) {
            if (!filter.accept(name, value)) {
                acceptTerm = false;

                break;
            }
        }

        if (acceptTerm) {
            Object thisVal = value;
            for (SearchTermsTransform transform : transforms) {
                Class<? extends SearchTermsTransform> cls = transform.getClass();
                if (cls.isAnnotationPresent(SearchTermType.class)) {
                    Class<?>[] classes = cls.getAnnotation(SearchTermType.class).value();

                    if (ArrayUtils.isNotEmpty(classes)) {
                        for (Class type : classes) {
                            if (type.isInstance(thisVal)) {
                                thisVal = transform.transform(name, thisVal);
                            }
                        }
                    }
                }
            }

            searchTerms.put(name, new SearchValue(value, thisVal));
        }
    }

    public void addSearchTerms(Map<String, Object> searchTerms) {
        if (isNotEmpty(searchTerms)) {
            Set<String> keySet = searchTerms.keySet();
            for (String key : keySet) {
                addSearchTerm(key, searchTerms.get(key));
            }
        }
    }

    public boolean removeSearchTerm(String name) {
        return searchTerms.remove(name) != null;
    }

    public boolean hasSearchTerm(String name) {
        return searchTerms.containsKey(name);
    }

    public List<Sort> getSorts() {
        return sorts;
    }

    public String encodeSorts() {
        if (CollectionUtils.isEmpty(sorts)) return "";

        StringBuffer buf = new StringBuffer();
        buf.append(" order by ");

        for (int i = 0; i < sorts.size(); i++) {
            if (i > 0) buf.append(", ");

            Sort sort = sorts.get(i);
            buf.append(sort.getName());
            buf.append(" ");
            buf.append(sort.getType());
        }

        return buf.toString();
    }

    public boolean addSort(Sort sort) {
        return sort != null && sorts.add(sort);
    }

    public boolean removeSort(Sort sort) {
        return sorts.remove(sort);
    }

    @Override
    public int getPageNumber() {
        return currentPage;
    }

    @Override
    public void setPageNumber(int currentPage) {
        this.currentPage = currentPage > 0 ? currentPage : 1;
    }

    @Override
    public List<SearchTermsFilter> getFilters() {
        return unmodifiableList(filters);
    }

    @Override
    public List<SearchTermsTransform> getTransforms() {
        return unmodifiableList(transforms);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, QueryStringEditor> getEditors() {
        return unmodifiableMap(editors);
    }

    @Override
    public void setFilters(List<SearchTermsFilter> filters) {
        if (CollectionUtils.isNotEmpty(filters)) {
            this.filters.addAll(filters);
        }
    }

    @Override
    public void setTransforms(List<SearchTermsTransform> transforms) {
        if (CollectionUtils.isNotEmpty(transforms)) {
            this.transforms.addAll(transforms);
        }
    }

    @Override
    public void setEditors(Map<String, QueryStringEditor> editors) {
        if (MapUtils.isNotEmpty(editors)) {
            this.editors.putAll(editors);
        }
    }
}
