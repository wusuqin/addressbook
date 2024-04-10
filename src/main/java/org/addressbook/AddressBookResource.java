package org.addressbook;

import io.netty.util.internal.StringUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.jaxrs.HeaderParam;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.service.ApplicationProperties;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("PROPFIND")
@interface PROPFIND {
}

@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("REPORT")
@interface REPORT {
}

@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("MKCOL")
@interface MKCOL {
}
@Path("/carddav/")
@Tag(name = "cardDav Service", description = "截获cardDav相关报文")
public class AddressBookResource {

    @Inject
    AddressBookService addressBookService;

    @Inject
    ApplicationProperties properties;

    @Inject
    CarddavPropService carddavPropService;

    @Inject
    UserInfoRepository userInfoRepository;

    static final String BAD_AUTH = "bad username or password";

    @PROPFIND
    @Path("/{id}/")
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_HTML})
    @Produces(MediaType.TEXT_XML)
    @Operation(description = "截获认证报文")
    public Response authMessageIntercept(@HeaderParam("Authorization") String security,
                                         @HeaderParam("Depth") Integer depth,
                                         @PathParam("id") String aoid,
                                         String body) {
        if (Objects.isNull(security)) {
            return Response.status(401).header("WWW-Authenticate", "Basic realm=\"Eulixos -Password Required\"").build();
        } else {
            var abAuthEntity = addressBookService.checkAuthResult(security);
            if(abAuthEntity == null) {
                return Response.status(Response.Status.FORBIDDEN).entity(BAD_AUTH).build();
            }
            var user = userInfoRepository.findByUserId(Long.valueOf(abAuthEntity.getUserId()));
            var doc = Jsoup.parse(body);
            var nodes = carddavPropService.getPropFindProps(doc.childNodes(), "propfind");
            List<Node> listResponse = new ArrayList<>();
            for(int i=0;i<= depth;i++) {
                listResponse.addAll(carddavPropService.getRootProp(nodes, user.getAoId(),i+1, null));
            }
            var document = carddavPropService.documentGen(listResponse);
            return Response.status(207).header("DAV","1, 2, 3, addressbook, extended-mkcol").
                    header("Server", "eulixos").entity(document.outerHtml()).build();
        }
    }

    @PROPFIND
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_HTML})
    @Produces(MediaType.TEXT_XML)
    @Operation(description = "截获认证报文，collection目录")
    public Response authMessageInterceptWithOutCollection(@HeaderParam("Authorization") String security,
                                                          @HeaderParam("Depth") Integer depth,
                                                          String body) {
        if (Objects.isNull(security)) {
            return Response.status(401).header("WWW-Authenticate", "Basic realm=\"Eulixos-Password Required\"").build();
        } else {
            var abAuthEntity = addressBookService.checkAuthResult(security);
            if(abAuthEntity == null) {
                return Response.status(Response.Status.FORBIDDEN).entity(BAD_AUTH).build();
            }
            var user = userInfoRepository.findByUserId(Long.valueOf(abAuthEntity.getUserId()));
            var doc = Jsoup.parse(body);
            var nodes = carddavPropService.getPropFindProps(doc.childNodes(), "propfind");
            List<Node> listResponse = new ArrayList<>();
            for(int i=0;i<= depth;i++) {
                listResponse.addAll(carddavPropService.getRootProp(nodes, user.getAoId(),i,null));
            }
            var document = carddavPropService.documentGen(listResponse);
            return Response.status(207).header("DAV", "1, 2, 3, addressbook, extended-mkcol").
                    header("server", "eulixos").
                    entity(document.outerHtml()).build();
        }
    }


    @OPTIONS
    @Operation(description = "截获principle resource请求")
    public Response checkAddressBookPrincipleResource(@HeaderParam("Authorization") String security) {
        if (Objects.isNull(security)) {
            return Response.status(401).header("WWW-Authenticate", "Basic realm=\"Eulixos -Password Required\"").build();
        }  else {
            var abAuthEntity = addressBookService.checkAuthResult(security);
            if(abAuthEntity == null) {
                return Response.status(Response.Status.FORBIDDEN).entity(BAD_AUTH).build();
            }
            return Response.status(200).
                    header("Allow", "DELETE, GET, HEAD, MERGE, MKCALENDAR, MKCOL, MOVE, OPTIONS, POST, PROPFIND, PROPPATCH, PUT, REPORT").
                    header("DAV","1, 2, 3, addressbook, extended-mkcol").
                    header("content-length", 0).
                    header("Server","eulixos").
                    build();
        }
    }

    @PUT
    @Path("/{id}/{vcfile}")
    @Consumes("text/vcard")
    public Response putVcfFile(@HeaderParam("If-None-Match") String ifNoneMatch,
                               @HeaderParam("If-Match") String ifMatch,
                               @PathParam("id") String aoId, @PathParam("vcfile") String vcFile, String body) {
        var value= carddavPropService.checkVcf(ifNoneMatch, ifMatch, aoId, vcFile, body);
        var status = Integer.valueOf(value.get(0));
        Response.ResponseBuilder builder = Response.status(status).header("Server", "eulixos");
        if(value.size() >= 2 && !StringUtil.isNullOrEmpty(value.get(1))) {
            builder.header("Etag", "\""+value.get(1) + "\"");
        }
        if(value.size() >= 3) {
            builder.entity(value.get(2));
        }
        return builder.build();
    }

    @REPORT
    @Path("/{id}/")
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_HTML})
    @Produces(MediaType.TEXT_XML)
    public Response reportVcfFile(@HeaderParam("Depth") Integer depth,
                                  @PathParam("id") String aoId, String body) {
        var user = userInfoRepository.findByAoId(aoId);
        var doc = Jsoup.parse(body);
        var nodesSync = carddavPropService.getReportProps(doc.childNodes(), "sync-collection");
        if(nodesSync.size() > 0) {
            var document = carddavPropService.documentGen(carddavPropService.getSyncValue(nodesSync, user.getAoId()));
            return Response.status(207).header("Server", "eulixos").entity(document.outerHtml()).build();
        }
        var nodes = carddavPropService.getReportProps(doc.childNodes(), "card:addressbook-multiget");
        if(nodes.size() > 0){
            var document = carddavPropService.documentGen(carddavPropService.getAddressMulti(nodes, user.getAoId()));
            var html = document.outerHtml();
            return Response.status(207).header("Server", "eulixos").header("Content-Length", html.getBytes(StandardCharsets.UTF_8).length).entity(html).build();
        }
        return Response.status(207).header("Server", "eulixos").build();
    }

    @DELETE
    @Path("/{id}/{vcfile}")
    @Produces(MediaType.TEXT_XML)
    public Response deleteVcfFile(@HeaderParam("If-Match") String ifMatch,
                                  @PathParam("id") String aoId, @PathParam("vcfile") String vcFile, String body) {
        var code = carddavPropService.deleteVcf(aoId, ifMatch,vcFile);
        if(code.equals("200")) {
            var document = carddavPropService.documentGen(List.of(carddavPropService.deletVcfResponse(aoId, vcFile, "HTTP/1.1 200 OK")));
            return Response.status(200).header("Server", "Eulixos").entity(document.outerHtml()).build();
        } else if (code.equals("412")) {
            return Response.status(Integer.valueOf(code)).entity("").build();
        } else{
            return Response.status(Integer.valueOf(code)).entity("Not found.").build();
        }
    }

	/*@MKCOL
	@Path("/{id}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response createAddressbook(@HeaderParam("Authorization") String security,
									  @PathParam("id") String id,  String body){
		if (Objects.isNull(security)) {
			return Response.status(401).header("WWW-Authenticate", "Basic realm=\"Eulixos -Password Required\"").build();
		}  else {
			var abAuthEntity = addressBookService.checkAuthResult(security);
			if(abAuthEntity == null) {
				return Response.status(Response.Status.FORBIDDEN).entity(BAD_AUTH).build();
			}
			try {
				var user = properties.radicaleapiAuth().split(":")[0];
				return cardDavService.createAddessbook(addressBookService.authInfo(), user, id,
						body.replace("<href>"+ADDRESSBOOK_PATH + "/", "<href>/" + user + "/"));
			} catch (ResteasyWebApplicationException ie) {
				return ie.getResponse();
			}
		}
	}*/

    /*@DELETE
    @Path("/{id}/")
    public Response deleteAddressBookCollection(@HeaderParam("Authorization") String security,
                                                @PathParam("id") String id) {
        if (Objects.isNull(security)) {
            return Response.status(401).header("WWW-Authenticate", "Basic realm=\"Eulixos -Password Required\"").build();
        }else {
            var abAuthEntity = addressBookService.checkAuthResult(security);
            if(abAuthEntity == null) {
                return Response.status(Response.Status.FORBIDDEN).entity(BAD_AUTH).build();
            }
            try {
                var user = properties.radicaleapiAuth().split(":")[0];
                //删除collection
                return cardDavService.deleteAddressBookCollection(addressBookService.authInfo(), user, id );
            } catch (ResteasyWebApplicationException ie) {
                return ie.getResponse();
            }
        }
    }*/
}
