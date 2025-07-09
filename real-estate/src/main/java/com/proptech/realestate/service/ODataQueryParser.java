package com.proptech.realestate.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing OData query parameters according to OData v4 specification
 * Supports RESO Web API requirements for MLS data access
 */
@Service
public class ODataQueryParser {
    
    // OData filter operators
    private static final Map<String, String> FILTER_OPERATORS = Map.of(
        "eq", "=",
        "ne", "!=", 
        "gt", ">",
        "ge", ">=",
        "lt", "<",
        "le", "<=",
        "and", "AND",
        "or", "OR",
        "not", "NOT"
    );
    
    // OData functions
    private static final Set<String> ODATA_FUNCTIONS = Set.of(
        "contains", "startswith", "endswith", "length", "indexof",
        "substring", "tolower", "toupper", "trim", "concat",
        "year", "month", "day", "hour", "minute", "second",
        "date", "time", "totaloffsetminutes", "now", "maxdatetime", "mindatetime"
    );
    
    /**
     * Complete OData query object
     */
    public static class ODataQuery {
        private FilterExpression filter;
        private List<String> select;
        private List<String> expand;
        private List<OrderByExpression> orderBy;
        private Integer top;
        private Integer skip;
        private String format;
        private Boolean count;
        
        // Getters and setters
        public FilterExpression getFilter() { return filter; }
        public void setFilter(FilterExpression filter) { this.filter = filter; }
        
        public List<String> getSelect() { return select; }
        public void setSelect(List<String> select) { this.select = select; }
        
        public List<String> getExpand() { return expand; }
        public void setExpand(List<String> expand) { this.expand = expand; }
        
        public List<OrderByExpression> getOrderBy() { return orderBy; }
        public void setOrderBy(List<OrderByExpression> orderBy) { this.orderBy = orderBy; }
        
        public Integer getTop() { return top; }
        public void setTop(Integer top) { this.top = top; }
        
        public Integer getSkip() { return skip; }
        public void setSkip(Integer skip) { this.skip = skip; }
        
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        
        public Boolean getCount() { return count; }
        public void setCount(Boolean count) { this.count = count; }
        
        /**
         * Convert to Spring Data Pageable
         */
        public Pageable toPageable() {
            int page = skip != null && top != null ? skip / top : 0;
            int size = top != null ? top : 20; // Default page size
            
            Sort sort = Sort.unsorted();
            if (orderBy != null && !orderBy.isEmpty()) {
                List<Sort.Order> orders = new ArrayList<>();
                for (OrderByExpression orderByExpr : orderBy) {
                    Sort.Direction direction = orderByExpr.isDescending() ? 
                        Sort.Direction.DESC : Sort.Direction.ASC;
                    orders.add(new Sort.Order(direction, orderByExpr.getField()));
                }
                sort = Sort.by(orders);
            }
            
            return PageRequest.of(page, size, sort);
        }
    }
    
    /**
     * Filter expression for $filter parameter
     */
    public static class FilterExpression {
        private String field;
        private String operator;
        private Object value;
        private FilterExpression left;
        private FilterExpression right;
        private String logicalOperator; // AND, OR
        private String function; // contains, startswith, etc.
        private List<Object> functionArgs;
        
        // Getters and setters
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
        
        public FilterExpression getLeft() { return left; }
        public void setLeft(FilterExpression left) { this.left = left; }
        
        public FilterExpression getRight() { return right; }
        public void setRight(FilterExpression right) { this.right = right; }
        
        public String getLogicalOperator() { return logicalOperator; }
        public void setLogicalOperator(String logicalOperator) { this.logicalOperator = logicalOperator; }
        
        public String getFunction() { return function; }
        public void setFunction(String function) { this.function = function; }
        
        public List<Object> getFunctionArgs() { return functionArgs; }
        public void setFunctionArgs(List<Object> functionArgs) { this.functionArgs = functionArgs; }
        
        public boolean isLogicalExpression() {
            return left != null && right != null && logicalOperator != null;
        }
        
        public boolean isFunctionExpression() {
            return function != null;
        }
    }
    
    /**
     * Order by expression for $orderby parameter
     */
    public static class OrderByExpression {
        private String field;
        private boolean descending;
        
        public OrderByExpression(String field, boolean descending) {
            this.field = field;
            this.descending = descending;
        }
        
        public String getField() { return field; }
        public boolean isDescending() { return descending; }
    }
    
    /**
     * Parse complete OData query from request parameters
     */
    public ODataQuery parseQuery(String filter, String select, String expand, 
                                String orderby, Integer top, Integer skip, 
                                String format, Boolean count) {
        ODataQuery query = new ODataQuery();
        
        if (filter != null && !filter.trim().isEmpty()) {
            query.setFilter(parseFilter(filter));
        }
        
        if (select != null && !select.trim().isEmpty()) {
            query.setSelect(Arrays.asList(select.split(",")));
        }
        
        if (expand != null && !expand.trim().isEmpty()) {
            query.setExpand(Arrays.asList(expand.split(",")));
        }
        
        if (orderby != null && !orderby.trim().isEmpty()) {
            query.setOrderBy(parseOrderBy(orderby));
        }
        
        query.setTop(top);
        query.setSkip(skip);
        query.setFormat(format);
        query.setCount(count);
        
        return query;
    }
    
    /**
     * Parse $filter parameter
     * Examples:
     * - ListPrice gt 500000
     * - City eq 'Seattle' and BedroomsTotal ge 3
     * - contains(City, 'San')
     */
    public FilterExpression parseFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return null;
        }
        
        filter = filter.trim();
        
        // Handle parentheses for grouping
        if (filter.startsWith("(") && filter.endsWith(")")) {
            return parseFilter(filter.substring(1, filter.length() - 1));
        }
        
        // Check for logical operators (AND, OR)
        FilterExpression logicalExpr = parseLogicalExpression(filter);
        if (logicalExpr != null) {
            return logicalExpr;
        }
        
        // Check for function calls
        FilterExpression functionExpr = parseFunctionExpression(filter);
        if (functionExpr != null) {
            return functionExpr;
        }
        
        // Parse simple comparison expression
        return parseComparisonExpression(filter);
    }
    
    /**
     * Parse logical expressions (AND, OR)
     */
    private FilterExpression parseLogicalExpression(String filter) {
        // Find logical operators at the top level (not inside parentheses or quotes)
        int andIndex = findTopLevelOperator(filter, " and ");
        int orIndex = findTopLevelOperator(filter, " or ");
        
        // Process AND first (higher precedence)
        if (andIndex != -1 && (orIndex == -1 || andIndex < orIndex)) {
            FilterExpression expr = new FilterExpression();
            expr.setLogicalOperator("AND");
            expr.setLeft(parseFilter(filter.substring(0, andIndex).trim()));
            expr.setRight(parseFilter(filter.substring(andIndex + 5).trim()));
            return expr;
        }
        
        // Process OR
        if (orIndex != -1) {
            FilterExpression expr = new FilterExpression();
            expr.setLogicalOperator("OR");
            expr.setLeft(parseFilter(filter.substring(0, orIndex).trim()));
            expr.setRight(parseFilter(filter.substring(orIndex + 4).trim()));
            return expr;
        }
        
        return null;
    }
    
    /**
     * Parse function expressions like contains(City, 'Seattle')
     */
    private FilterExpression parseFunctionExpression(String filter) {
        Pattern functionPattern = Pattern.compile("(\\w+)\\s*\\((.+)\\)");
        Matcher matcher = functionPattern.matcher(filter);
        
        if (matcher.matches()) {
            String functionName = matcher.group(1);
            String argsString = matcher.group(2);
            
            if (ODATA_FUNCTIONS.contains(functionName.toLowerCase())) {
                FilterExpression expr = new FilterExpression();
                expr.setFunction(functionName);
                expr.setFunctionArgs(parseFunctionArguments(argsString));
                return expr;
            }
        }
        
        return null;
    }
    
    /**
     * Parse simple comparison expressions like "ListPrice gt 500000"
     */
    private FilterExpression parseComparisonExpression(String filter) {
        // Find comparison operators
        for (Map.Entry<String, String> entry : FILTER_OPERATORS.entrySet()) {
            String odataOp = entry.getKey();
            if (filter.contains(" " + odataOp + " ")) {
                int opIndex = filter.indexOf(" " + odataOp + " ");
                
                FilterExpression expr = new FilterExpression();
                expr.setField(filter.substring(0, opIndex).trim());
                expr.setOperator(entry.getValue());
                expr.setValue(parseValue(filter.substring(opIndex + odataOp.length() + 2).trim()));
                return expr;
            }
        }
        
        return null;
    }
    
    /**
     * Parse $orderby parameter
     * Examples: 
     * - ListPrice desc
     * - City asc, ListPrice desc
     */
    private List<OrderByExpression> parseOrderBy(String orderby) {
        List<OrderByExpression> expressions = new ArrayList<>();
        
        String[] parts = orderby.split(",");
        for (String part : parts) {
            part = part.trim();
            
            boolean descending = false;
            String field = part;
            
            if (part.endsWith(" desc")) {
                descending = true;
                field = part.substring(0, part.length() - 5).trim();
            } else if (part.endsWith(" asc")) {
                field = part.substring(0, part.length() - 4).trim();
            }
            
            expressions.add(new OrderByExpression(field, descending));
        }
        
        return expressions;
    }
    
    /**
     * Find top-level operator (not inside parentheses or quotes)
     */
    private int findTopLevelOperator(String filter, String operator) {
        int parenthesesLevel = 0;
        boolean inQuotes = false;
        char quoteChar = 0;
        
        for (int i = 0; i <= filter.length() - operator.length(); i++) {
            char c = filter.charAt(i);
            
            if (!inQuotes) {
                if (c == '(' || c == '[') {
                    parenthesesLevel++;
                } else if (c == ')' || c == ']') {
                    parenthesesLevel--;
                } else if (c == '\'' || c == '"') {
                    inQuotes = true;
                    quoteChar = c;
                } else if (parenthesesLevel == 0 && 
                          filter.substring(i).startsWith(operator)) {
                    return i;
                }
            } else {
                if (c == quoteChar && (i == 0 || filter.charAt(i - 1) != '\\')) {
                    inQuotes = false;
                }
            }
        }
        
        return -1;
    }
    
    /**
     * Parse function arguments
     */
    private List<Object> parseFunctionArguments(String argsString) {
        List<Object> args = new ArrayList<>();
        
        // Simple parsing - split by comma (not inside quotes)
        StringBuilder currentArg = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;
        
        for (char c : argsString.toCharArray()) {
            if (!inQuotes) {
                if (c == '\'' || c == '"') {
                    inQuotes = true;
                    quoteChar = c;
                    currentArg.append(c);
                } else if (c == ',') {
                    args.add(parseValue(currentArg.toString().trim()));
                    currentArg = new StringBuilder();
                } else {
                    currentArg.append(c);
                }
            } else {
                currentArg.append(c);
                if (c == quoteChar) {
                    inQuotes = false;
                }
            }
        }
        
        if (currentArg.length() > 0) {
            args.add(parseValue(currentArg.toString().trim()));
        }
        
        return args;
    }
    
    /**
     * Parse value and convert to appropriate type
     */
    private Object parseValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        value = value.trim();
        
        // String literal (quoted)
        if ((value.startsWith("'") && value.endsWith("'")) ||
            (value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        }
        
        // Boolean
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        
        // Null
        if ("null".equalsIgnoreCase(value)) {
            return null;
        }
        
        // Numeric (try integer first, then decimal)
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            // Not a number, return as string
            return value;
        }
    }
    
    /**
     * Validate RESO field names for security
     */
    public boolean isValidResoField(String fieldName) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return false;
        }
        
        // Basic validation - only allow alphanumeric, underscore, and dot notation
        return fieldName.matches("^[a-zA-Z][a-zA-Z0-9_.]*$");
    }
    
    /**
     * Convert RESO field name to internal entity field name
     */
    public String mapResoFieldToInternal(String resoField) {
        // Map common RESO fields to internal field names
        Map<String, String> fieldMapping = Map.of(
            "ListingKey", "id",
            "ListPrice", "price",
            "BedroomsTotal", "numBedrooms",
            "BathroomsTotalInteger", "numBathrooms",
            "LivingArea", "area",
            "City", "city",
            "StateOrProvince", "state",
            "PostalCode", "zipCode",
            "StandardStatus", "status",
            "PublicRemarks", "description",
            "ModificationTimestamp", "updatedAt"
        );
        
        return fieldMapping.getOrDefault(resoField, resoField.toLowerCase());
    }
}
