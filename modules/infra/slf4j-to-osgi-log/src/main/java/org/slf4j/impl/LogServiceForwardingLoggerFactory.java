/**
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 * <p>
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 * <p>
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LogServiceForwardingLoggerFactory implements ILoggerFactory {

    private final ConcurrentMap<String, Logger> loggerMap;
    private final OsgiLogServiceMessageForwarder messageForwarder;

    public LogServiceForwardingLoggerFactory() {
        loggerMap = new ConcurrentHashMap<>();
        messageForwarder = new OsgiLogServiceMessageForwarder();
    }

    public Logger getLogger( String name ) {
        Logger logger = loggerMap.get( name );
        if ( logger != null ) {
            return logger;
        } else {
            Logger newInstance = new LogServiceForwardingLogger( name, messageForwarder );
            Logger oldInstance = loggerMap.putIfAbsent( name, newInstance );
            return oldInstance == null ? newInstance : oldInstance;
        }
    }

}
