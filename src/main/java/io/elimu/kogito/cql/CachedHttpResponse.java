// Copyright 2018-2024 Elimu Informatics
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.elimu.kogito.cql;

import java.io.IOException;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;
import org.apache.http.params.HttpParams;

@SuppressWarnings("deprecation")
public class CachedHttpResponse implements HttpResponse {

    private StatusLine statusline;
    private ProtocolVersion ver;
    private StringEntity entity;
    private Locale locale;
	private HeaderGroup headergroup;
	private HttpParams params;
	
	public CachedHttpResponse(HttpResponse response) throws IOException {
		this.statusline = response.getStatusLine();
		this.ver = statusline.getProtocolVersion();
        this.headergroup = new HeaderGroup();
        this.headergroup.setHeaders(response.getAllHeaders());
        this.params = response.getParams();
        this.locale = response.getLocale();
        this.entity = new StringEntity(new String(response.getEntity().getContent().readAllBytes()));
        this.entity.setContentEncoding(response.getEntity().getContentEncoding());
        this.entity.setContentType(response.getEntity().getContentType());
	}
	
	@Override
	public ProtocolVersion getProtocolVersion() {
		return this.ver;
	}

	@Override
	public boolean containsHeader(String name) {
		return this.headergroup.containsHeader(name);
	}

	@Override
	public Header[] getHeaders(String name) {
		return this.headergroup.getHeaders(name);
	}

	@Override
	public Header getFirstHeader(String name) {
		return this.headergroup.getFirstHeader(name);
	}

	@Override
	public Header getLastHeader(String name) {
		return this.headergroup.getLastHeader(name);
	}

	@Override
	public Header[] getAllHeaders() {
		return this.headergroup.getAllHeaders();
	}

	@Override
	public void addHeader(Header header) {
		this.headergroup.addHeader(header);
	}

	@Override
	public void addHeader(String name, String value) {
		this.headergroup.addHeader(new BasicHeader(name, value));
	}

	@Override
	public void setHeader(Header header) {
		this.headergroup.updateHeader(header);

	}

	@Override
	public void setHeader(String name, String value) {
		this.headergroup.updateHeader(new BasicHeader(name, value));
	}

	@Override
	public void setHeaders(Header[] headers) {
		this.headergroup.setHeaders(headers);
	}

	@Override
	public void removeHeader(Header header) {
		this.headergroup.removeHeader(header);
	}

	@Override
	public void removeHeaders(String name) {
        if (name == null) {
            return;
        }
        for (final HeaderIterator i = this.headergroup.iterator(); i.hasNext(); ) {
            final Header header = i.nextHeader();
            if (name.equalsIgnoreCase(header.getName())) {
                i.remove();
            }
        }
	}

	@Override
	public HeaderIterator headerIterator() {
		return this.headergroup.iterator();
	}

	@Override
	public HeaderIterator headerIterator(String name) {
		return this.headergroup.iterator(name);
	}

	@Override
	public HttpParams getParams() {
		return params;
	}

	@Override
	public void setParams(HttpParams params) {
		this.params = params;

	}

	@Override
	public StatusLine getStatusLine() {
		return statusline;
	}

	@Override
	public void setStatusLine(StatusLine statusline) {
		this.ver = statusline.getProtocolVersion();
	}

	@Override
	public void setStatusLine(ProtocolVersion ver, int code) {
        this.statusline = null;
        this.ver = ver;
	}

	@Override
	public void setStatusLine(ProtocolVersion ver, int code, String reason) {
        this.statusline = null;
        this.ver = ver;
	}

	@Override
	public void setStatusCode(int code) throws IllegalStateException {
        this.statusline = null;
	}

	@Override
	public void setReasonPhrase(String reason) throws IllegalStateException {
        this.statusline = null;
	}

	@Override
	public HttpEntity getEntity() {
		return this.entity;
	}

	@Override
	public void setEntity(HttpEntity entity) {
		if (entity instanceof StringEntity) {
			this.entity = (StringEntity) entity;
		} else {
			try {
				this.entity = new StringEntity(new String(entity.getContent().readAllBytes()));
			} catch (IOException e) {
				throw new IllegalArgumentException("entity must have content");
			}
		}
        this.entity.setContentEncoding(entity.getContentEncoding());
        this.entity.setContentType(entity.getContentType());
	}

	@Override
	public Locale getLocale() {
		return this.locale;
	}

	@Override
	public void setLocale(Locale loc) {
		this.locale = loc;
	}
}
