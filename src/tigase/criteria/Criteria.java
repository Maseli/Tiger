/*  Tigase Project
 *  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev: 644 $
 * Last modified by $Author: wojtek $
 * $Date: 2012-08-21 06:20:24 +0800 (Tue, 21 Aug 2012) $
 */
package tigase.criteria;

import tigase.xml.Element;

/**
 * 
 * <p>
 * Created: 2007-06-20 08:03:14
 * </p>
 * 
 * @author bmalkow
 * @version $Rev: 644 $
 */
public interface Criteria {

	Criteria add(Criteria criteria);

	boolean match(Element element);
}