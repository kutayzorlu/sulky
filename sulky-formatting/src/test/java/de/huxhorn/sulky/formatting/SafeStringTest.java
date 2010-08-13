/*
 * sulky-modules - several general-purpose modules.
 * Copyright (C) 2007-2010 Joern Huxhorn
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Copyright 2007-2010 Joern Huxhorn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.huxhorn.sulky.formatting;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SafeStringTest
{
	private final Logger logger = LoggerFactory.getLogger(SafeStringTest.class);

	@SuppressWarnings({"unchecked"})
	@Test(expected = StackOverflowError.class)
	public void showMapRecursionProblem()
	{
		Map a = new HashMap();
		Map b = new HashMap();
		b.put("bar", a);
		a.put("foo", b);
		// the following line will throw an java.lang.StackOverflowError!
		a.toString();
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void verifyMapRecursionWorks()
	{
		Map a = new HashMap();
		Map b = new HashMap();
		b.put("bar", a);
		a.put("foo", b);

		String expected = "{foo={bar=" + SafeString.RECURSION_PREFIX +
			SafeString.identityToString(a) + SafeString.RECURSION_SUFFIX + "}}";

		evaluate(expected, a);
	}

	@SuppressWarnings({"unchecked"})
	@Test(expected = java.lang.StackOverflowError.class)
	public void showCollectionRecursionProblem()
	{
		List a = new ArrayList();
		List b = new ArrayList();
		b.add(a);
		a.add(b);
		// the following line will throw an java.lang.StackOverflowError!
		a.toString();
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void verifyCollectionRecursionWorks()
	{
		List a = new ArrayList();
		List b = new ArrayList();
		b.add(a);
		a.add(b);

		String expected = "[[" + SafeString.RECURSION_PREFIX +
				SafeString.identityToString(a) + SafeString.RECURSION_SUFFIX + "]]";

		evaluate(expected, a);
	}

	@SuppressWarnings({"ResultOfMethodCallIgnored"})
	@Test(expected = FooThrowable.class)
	public void showExceptionInToStringProblem()
	{
		ProblematicToString problem=new ProblematicToString();
		// the following line will throw a FooThrowable
		String.valueOf(problem);
	}

	@Test
	public void verifyExceptionInToStringWorks()
	{
		ProblematicToString o = new ProblematicToString();
		String expected = SafeString.ERROR_PREFIX + SafeString.identityToString(o)
			+ SafeString.ERROR_SEPARATOR + FooThrowable.class.getName()
			+ SafeString.ERROR_MSG_SEPARATOR
			+ "FooThrowable"
			+ SafeString.ERROR_SUFFIX;

		evaluate(expected, o);
	}

	@Test
	public void deepMapList()
	{
		List<String> list = new ArrayList<String>();
		list.add("One");
		list.add("Two");
		Map<String, List<String>> map = new TreeMap<String, List<String>>();
		map.put("foo", list);
		map.put("bar", list);

		evaluate("{bar=[One, Two], foo=[One, Two]}", map);
	}

	@Test
	public void deepMapArray()
	{
		String[] array = new String[]{"One", "Two"};
		Map<String, String[]> map = new TreeMap<String, String[]>();
		map.put("foo", array);
		map.put("bar", array);

		evaluate("{bar=[One, Two], foo=[One, Two]}", map);
	}

	@Test
	public void deepListList()
	{
		List<String> list = new ArrayList<String>();
		list.add("One");
		list.add("Two");
		List<List<String>> outer = new ArrayList<List<String>>();
		outer.add(list);
		outer.add(list);

		evaluate("[[One, Two], [One, Two]]", outer);
	}

	@Test
	public void deepListArray()
	{
		String[] array = new String[]{"One", "Two"};
		List<String[]> list = new ArrayList<String[]>();
		list.add(array);
		list.add(array);

		evaluate("[[One, Two], [One, Two]]", list);
	}

	@Test
	public void date()
	{
		String result;
		String expected;
		Object o;

		{
			o = new Date(1234567890000L);
			expected = "2009-02-14T00:31:30.000";
		}
		if(logger.isInfoEnabled()) logger.info("Evaluating {}...", o);
		result = SafeString.toString(o);
		if(logger.isInfoEnabled()) logger.info("Result of {} is {}.", o, result);
		assertTrue(result.startsWith(expected));
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void verifyNull()
	{
		evaluate(null, null);
	}

	private void evaluate(String expected, Object o)
	{
		String result;
		if(logger.isInfoEnabled()) logger.info("Evaluating {}...", o);
		result = SafeString.toString(o);
		if(logger.isInfoEnabled()) logger.info("Result of {} is {}.", o, result);
		assertEquals(expected, result);
	}

	private static class ProblematicToString
	{
		public String toString()
		{
			throw new FooThrowable("FooThrowable");
		}
	}

	private static class FooThrowable
		extends RuntimeException
	{
		private static final long serialVersionUID = 9140989200041952994L;

		public FooThrowable(String s)
		{
			super(s);
		}

		@Override
		public String toString()
		{
			return "" + getMessage();
		}
	}
}
