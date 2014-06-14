/**
 * Copyright (c) 2012-2013, Michael Yang 杨福海 (www.yangfuhai.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.police.bjxj.accessor;

import net.tsz.afinal.exception.AfinalException;

public class BjjxException extends AfinalException {
	private static final long serialVersionUID = 1L;
	
	/**
	 * 默认error code
	 */
	public static final int NETWORK_ERROR_CODE = Integer.MAX_VALUE;

	
	public BjjxException(int errorCode, String msg, Throwable ex) {
		super(msg, ex);
		code = errorCode;
	}

	public BjjxException(int errorCode, String msg) {
		super(msg);
		code = errorCode;
	}

	/**
	 * 网络返回错误码
	 */
	private int code;
	
	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}
}
