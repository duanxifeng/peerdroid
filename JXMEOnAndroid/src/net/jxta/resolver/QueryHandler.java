/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *       Sun Microsystems, Inc. for Project JXTA."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact Project JXTA at http://www.jxta.org.
 *
 * 5. Products derived from this software may not be called "JXTA",
 *    nor may "JXTA" appear in their name, without prior written
 *    permission of Sun.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: QueryHandler.java,v 1.2 2005/05/14 16:11:42 hamada Exp $
 */

package net.jxta.resolver;

import net.jxta.protocol.ResolverQueryMsg;
import net.jxta.protocol.ResolverResponseMsg;
import net.jxta.endpoint.EndpointAddress;

/**
 * Services that wish to act as a resolver handler must implement this interface
 *
 * @see net.jxta.resolver.ResolverService
 * @see net.jxta.resolver.GenericResolver
 * @see net.jxta.protocol.ResolverQueryMsg
 *
 **/
public interface QueryHandler {
    
    /**
     * Process the resolver query, and generate response
     * it is the responsibilty of the handler to send the response
     *
     * <p/><pre>
     * result = processIncommingQuery(query);
     * if (result != null) {
     *   resolver.sendResponse(query.getSrc(), response);
     *   return ResolverService.OK;
     *  } else return ResolverService.Repropagate;
     * </pre>
     *
     * @param query ResolverQueryMsg query
     * @return int status, {@link net.jxta.resolver.ResolverService#OK OK} success,
     {@link net.jxta.resolver.ResolverService#Repropagate Repropagate} to
     * indicate a re-propagation is needed
     */
    
    public int processQuery(ResolverQueryMsg query, EndpointAddress srcAddr);
    
    /**
     * Called when messages are received by the ResolverService
     * it calls back this method to deal with received responses
     *
     * @param response ResolverQueryMsg reponse
     *
     **/
    public void processResponse(ResolverResponseMsg response, EndpointAddress srcAddr);
    
}
