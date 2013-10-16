/***
 * Copyright (c) 2009 Caelum - www.caelum.com.br/opensource
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.com.caelum.vraptor.view;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.inject.Inject;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.caelum.vraptor.controller.DefaultControllerMethod;
import br.com.caelum.vraptor.core.MethodInfo;
import br.com.caelum.vraptor.http.MutableRequest;
import br.com.caelum.vraptor.http.MutableResponse;
import br.com.caelum.vraptor.interceptor.ApplicationLogicException;
import br.com.caelum.vraptor.proxy.MethodInvocation;
import br.com.caelum.vraptor.proxy.Proxifier;
import br.com.caelum.vraptor.proxy.ProxyInvocationException;
import br.com.caelum.vraptor.proxy.SuperMethod;

/**
 * Default page result implementation.
 *
 * @author Guilherme Silveira
 * @author Lucas Cavalcanti
 */
public class DefaultPageResult implements PageResult {

	private static final Logger logger = LoggerFactory.getLogger(DefaultPageResult.class);

	private final MutableRequest request;
	private final MutableResponse response;
	private final PathResolver resolver;
	private final Proxifier proxifier;
	private final MethodInfo requestInfo;
	
	/** @Deprecated CDI eyes only */
	public DefaultPageResult() {
		this(null, null, null, null, null);
	}

	@Inject
	public DefaultPageResult(MutableRequest req, MutableResponse res, MethodInfo requestInfo,
			PathResolver resolver, Proxifier proxifier) {
		this.request = req;
		this.response = res;
		this.requestInfo = requestInfo;
		this.proxifier = proxifier;
		this.resolver = resolver;
	}

	@Override
	public void defaultView() {
		String to = resolver.pathFor(requestInfo.getControllerMethod());
		logger.debug("forwarding to {}", to);
		try {
			request.getRequestDispatcher(to).forward(request, response);
		} catch (ServletException e) {
			throw new ApplicationLogicException(to + " raised an exception", e);
		} catch (IOException e) {
			throw new ResultException(e);
		}
	}

	@Override
	public void include() {
		try {
			String to = resolver.pathFor(requestInfo.getControllerMethod());
			logger.debug("including {}", to);
			request.getRequestDispatcher(to).include(request, response);
		} catch (ServletException e) {
			throw new ResultException(e);
		} catch (IOException e) {
			throw new ResultException(e);
		}
	}

	@Override
	public void redirectTo(String url) {
		logger.debug("redirection to {}", url);

		try {
			if (url.startsWith("/")) {
				response.sendRedirect(request.getContextPath() + url);
			} else {
				response.sendRedirect(url);
			}
		} catch (IOException e) {
			throw new ResultException(e);
		}
	}

	@Override
	public void forwardTo(String url) {
		logger.debug("forwarding to {}", url);

		try {
			request.getRequestDispatcher(url).forward(request, response);
		} catch (ServletException | IOException e) {
			throw new ResultException(e);
		}
	}

	@Override
	public <T> T of(final Class<T> controllerType) {
		return proxifier.proxify(controllerType, new MethodInvocation<T>() {
			@Override
			public Object intercept(T proxy, Method method, Object[] args, SuperMethod superMethod) {
				try {
					request.getRequestDispatcher(
							resolver.pathFor(DefaultControllerMethod.instanceFor(controllerType, method))).forward(
							request, response);
					return null;
				} catch (Exception e) {
					throw new ProxyInvocationException(e);
				}
			}
		});
	}

}
