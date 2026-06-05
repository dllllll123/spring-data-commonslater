/*
 * Copyright 2024-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.expression;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;
import org.springframework.util.SystemPropertyUtils;

/**
 * Default {@link ValueExpressionParser} implementation. Instances are thread-safe.
 *
 * @author Mark Paluch
 * @since 3.3
 */
class DefaultValueExpressionParser implements ValueExpressionParser {

	public static final String PLACEHOLDER_PREFIX = SystemPropertyUtils.PLACEHOLDER_PREFIX;
	public static final String EXPRESSION_PREFIX = ParserContext.TEMPLATE_EXPRESSION.getExpressionPrefix();
	public static final char SUFFIX = '}';
	public static final int PLACEHOLDER_PREFIX_LENGTH = PLACEHOLDER_PREFIX.length();
	public static final char[] QUOTE_CHARS = { '\'', '"' };

	public static final ValueExpressionParser DEFAULT = new DefaultValueExpressionParser(SpelExpressionParser::new);

	private final ValueParserConfiguration configuration;

	public DefaultValueExpressionParser(ValueParserConfiguration configuration) {

		Assert.notNull(configuration, "ValueParserConfiguration must not be null");

		this.configuration = configuration;
	}

	@Override
	public ValueExpression parse(String expressionString) {

		int placeholderIndex = expressionString.indexOf(PLACEHOLDER_PREFIX);
		int expressionIndex = expressionString.indexOf(EXPRESSION_PREFIX);

		if (isLiteralExpression(placeholderIndex, expressionIndex)) {
			return new LiteralValueExpression(expressionString);
		}

		if (canCreatePlaceholder(expressionString, placeholderIndex, expressionIndex)) {
			return createPlaceholder(expressionString);
		}

		if (canCreateExpression(expressionString, placeholderIndex)) {
			return createExpression(expressionString);
		}

		return parseComposite(expressionString, placeholderIndex, expressionIndex);
	}

	private static boolean isLiteralExpression(int placeholderIndex, int expressionIndex) {
		return placeholderIndex == -1 && expressionIndex == -1;
	}

	private static boolean canCreatePlaceholder(String expressionString, int placeholderIndex, int expressionIndex) {
		return placeholderIndex != -1 && expressionIndex == -1
				&& findExpressionEndIndex(expressionString, placeholderIndex) != expressionString.length();
	}

	private static boolean canCreateExpression(String expressionString, int placeholderIndex) {
		return placeholderIndex == -1 && findExpressionEndIndex(expressionString,
				expressionString.indexOf(EXPRESSION_PREFIX)) != expressionString.length();
	}

	private CompositeValueExpression parseComposite(String expressionString, int placeholderIndex, int expressionIndex) {

		List<ValueExpression> expressions = new ArrayList<>(PLACEHOLDER_PREFIX_LENGTH);
		int startIndex = getStartIndex(placeholderIndex, expressionIndex);

		addLeadingLiteral(expressions, expressionString, startIndex);

		while (startIndex != -1) {

			int endIndex = findExpressionEndIndexOrThrow(expressionString, startIndex);
			int nextStartIndex = findNextExpressionStart(expressionString, endIndex);

			expressions.add(createExpression(expressionString, startIndex, endIndex));
			expressions.add(createLiteral(expressionString, endIndex + 1, nextStartIndex));

			startIndex = nextStartIndex;
		}

		return new CompositeValueExpression(expressionString, expressions);
	}

	private static void addLeadingLiteral(List<ValueExpression> expressions, String expressionString, int startIndex) {

		if (startIndex != 0) {
			expressions.add(new LiteralValueExpression(expressionString.substring(0, startIndex)));
		}
	}

	private static int findNextExpressionStart(String expressionString, int currentEndIndex) {

		int placeholderIndex = expressionString.indexOf(PLACEHOLDER_PREFIX, currentEndIndex);
		int expressionIndex = expressionString.indexOf(EXPRESSION_PREFIX, currentEndIndex);
		return getStartIndex(placeholderIndex, expressionIndex);
	}

	private static LiteralValueExpression createLiteral(String expressionString, int startIndex, int nextStartIndex) {

		int endIndex = nextStartIndex == -1 ? expressionString.length() : nextStartIndex;
		return new LiteralValueExpression(expressionString.substring(startIndex, endIndex));
	}

	private ValueExpression createExpression(String expressionString, int startIndex, int endIndex) {

		String part = expressionString.substring(startIndex, endIndex + 1);
		return part.startsWith(PLACEHOLDER_PREFIX) ? createPlaceholder(part) : createExpression(part);
	}

	private static int getStartIndex(int placeholderIndex, int expressionIndex) {
		return placeholderIndex != -1 && expressionIndex != -1 ? Math.min(placeholderIndex, expressionIndex)
				: placeholderIndex != -1 ? placeholderIndex : expressionIndex;
	}

	private PlaceholderExpression createPlaceholder(String part) {
		return new PlaceholderExpression(part);
	}

	private ExpressionExpression createExpression(String expression) {

		Expression expr = configuration.getExpressionParser().parseExpression(expression,
				ParserContext.TEMPLATE_EXPRESSION);
		ExpressionDependencies dependencies = ExpressionDependencies.discover(expr);
		return new ExpressionExpression(expr, dependencies);
	}

	private static int findExpressionEndIndexOrThrow(String expressionString, int startIndex) {

		int endIndex = findExpressionEndIndex(expressionString, startIndex);
		if (endIndex == -1) {
			throw new ParseException(expressionString, startIndex,
					"No ending suffix '}' for expression starting at character %d: %s".formatted(startIndex,
							expressionString.substring(startIndex)));
		}
		return endIndex;
	}

	private static int findExpressionEndIndex(CharSequence buf, int startIndex) {

		int index = startIndex + PLACEHOLDER_PREFIX_LENGTH;
		char quotationChar = 0;
		char nestingLevel = 0;
		boolean skipEscape = false;

		while (index < buf.length()) {

			char c = buf.charAt(index);

			if (!skipEscape && c == '\\') {
				skipEscape = true;
			} else if (skipEscape) {
				skipEscape = false;
			} else if (quotationChar == 0) {

				for (char quoteChar : QUOTE_CHARS) {
					if (quoteChar == c) {
						quotationChar = c;
						break;
					}
				}
			} else if (quotationChar == c) {
				quotationChar = 0;
			}

			if (!skipEscape && quotationChar == 0) {

				if (nestingLevel != 0 && c == SUFFIX) {
					nestingLevel--;
				} else if (c == '{') {
					nestingLevel++;
				} else if (nestingLevel == 0 && c == SUFFIX) {
					return index;
				}
			}

			index++;
		}

		return -1;
	}
}
