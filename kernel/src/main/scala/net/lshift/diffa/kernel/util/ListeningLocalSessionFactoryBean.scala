/**
 * Copyright (C) 2010 LShift Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lshift.diffa.kernel.util

import org.springframework.orm.hibernate3.LocalSessionFactoryBean
import org.hibernate.SessionFactoryObserver

/**
 * This wires in a callback that will be invoked when the underlying session factory has been
 * created. This is a subclass of the Spring template because that template doesn't expose
 * the Hibernate Configuration object in a suitable way. 
 */
class ListeningLocalSessionFactoryBean(observer:SessionFactoryObserver) extends LocalSessionFactoryBean {

  override def newConfiguration() = {
    val config = super.newConfiguration()
    config.setSessionFactoryObserver(observer)
    config
	}
}