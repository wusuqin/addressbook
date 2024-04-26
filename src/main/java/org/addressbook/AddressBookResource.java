package org.addressbook;

import io.netty.util.internal.StringUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.addressbook.service.*;
import org.dom4j.DocumentHelper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jsoup.Jsoup;

import java.io.ByteArrayOutputStream;
import org.dom4j.io.XMLWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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

@Tag(name = "cardDav Service", description = "截获cardDav相关报文")
@Path("carddav")
public class AddressBookResource {

    @Inject
    AuthService authService;

    @Inject
    CarddavPropservice carddavPropService;

    @Inject
    CarddavCollectService carddavCollectService;

    @Inject
    BaseService baseService;

    @Inject
    RootPropService rootPropService;

    static final String SERVER = "personal addressbook service";

    static final String BAD_AUTH = "bad username or password";

    @PROPFIND
    @Path("/{user}/{collectId}/")
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_HTML})
    @Produces(MediaType.TEXT_HTML)
    @Operation(description = "截获认证报文")
    public Response authMessageIntercept(@HeaderParam("Authorization") String security,
                                         @HeaderParam("Depth") Integer depth,
                                         @PathParam("user") String user,
                                         @PathParam("collectId") String collectId,
                                         String body) {
        if (Objects.isNull(security)) {
            return Response.status(401).header("WWW-Authenticate", "Basic realm=\"Addressbook -Password Required\"").build();
        } else {
            var userPrinciple = authService.checkAuthResult(security);
            if(Objects.isNull(userPrinciple)) {
                return Response.status(Response.Status.FORBIDDEN).entity(BAD_AUTH).build();
            }
            var doc = Jsoup.parse(body);
            var nodes = carddavPropService.getPropFindProps(doc.childNodes(), "propfind");
            var document = DocumentHelper.createDocument();
            var multistatus = baseService.multistatusGen(document);
            rootPropService.hrefResponseGen(nodes, userPrinciple, user, collectId,null, 2,multistatus);
            if(depth >=1) {
                rootPropService.hrefResponseGen(nodes, userPrinciple, user, collectId,null, 3, multistatus);
            }
            try{
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                XMLWriter writer = new XMLWriter(outputStream);
                writer.write(document);
                writer.close();
                return Response.status(207).header("DAV","1, 2, 3, addressbook, extended-mkcol").
                        header("Server", SERVER).entity(outputStream.toByteArray()).build();
            }catch (Exception ie) {
                return Response.status(405).entity("The method is not allowed on the requested resource.").build();
            }
        }
    }

    @PROPFIND
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_HTML})
    @Produces(MediaType.TEXT_HTML)
    @Operation(description = "截获认证报文，collection目录")
    @Path("/{user}/")
    public Response principleUserPropFind(@HeaderParam("Authorization") String security,
                                                          @HeaderParam("Depth") Integer depth,
                                                          @PathParam("user") String user,
                                                          String body) {
        if (Objects.isNull(security)) {
            return Response.status(401).header("WWW-Authenticate", "Basic realm=\"Addressbook-Password Required\"").build();
        } else {
            var userPrinciple = authService.checkAuthResult(security);
            if(Objects.isNull(userPrinciple)) {
                return Response.status(Response.Status.FORBIDDEN).entity(BAD_AUTH).build();
            }
            var doc = Jsoup.parse(body);
            var nodes = carddavPropService.getPropFindProps(doc.childNodes(), "propfind");
            var document = DocumentHelper.createDocument();
            var multistatus = baseService.multistatusGen(document);
            rootPropService.hrefResponseGen(nodes, userPrinciple, user, null,null,1, multistatus);
            if(depth >=1) {
                BaseService.PROPS.forEach( (key, value) -> {
                    if(key.startsWith(user)) {
                        var collectId = key.split(LockService.LOCK_PREFIX)[1];
                        rootPropService.hrefResponseGen(nodes, userPrinciple, user, collectId,null, 2, multistatus);
                    }
                });
            }
            try{
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                XMLWriter writer = new XMLWriter(outputStream);
                writer.write(document);
                writer.close();
                return Response.status(207).header("DAV","1, 2, 3, addressbook, extended-mkcol").
                        header("Server", SERVER).entity(outputStream.toByteArray()).build();
            }catch (Exception ie) {
                return Response.status(405).entity("The method is not allowed on the requested resource.").build();
            }
        }
    }

    @PROPFIND
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_HTML})
    @Produces(MediaType.TEXT_HTML)
    public Response rootPropFind(@HeaderParam("Authorization") String security,
                             @HeaderParam("Depth") Integer depth,
                             String body) {
        if (Objects.isNull(security)) {
            return Response.status(401).header("WWW-Authenticate", "Basic realm=\"Addressbook-Password Required\"").build();
        } else {
            var userPrinciple = authService.checkAuthResult(security);
            if(Objects.isNull(userPrinciple)) {
                return Response.status(Response.Status.FORBIDDEN).entity(BAD_AUTH).build();
            }
            var doc = Jsoup.parse(body);
            var nodes = carddavPropService.getPropFindProps(doc.childNodes(), "propfind");
            var document = DocumentHelper.createDocument();
            var multistatus = baseService.multistatusGen(document);
            rootPropService.hrefResponseGen(nodes, userPrinciple, null, null,null, 0, multistatus);
            if(depth >= 1) {
                for(var user:authService.currentUsers()) {
                    rootPropService.hrefResponseGen(nodes, userPrinciple, user, null, null, 1, multistatus);
                }
            }
            try{
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                XMLWriter writer = new XMLWriter(outputStream);
                writer.write(document);
                writer.close();
                return Response.status(207).header("DAV","1, 2, 3, addressbook, extended-mkcol").
                        header("Server", SERVER).entity(outputStream.toByteArray()).build();
            }catch (Exception ie) {
                return Response.status(405).entity("The method is not allowed on the requested resource.").build();
            }
        }
    }

    @OPTIONS
    @Path("/{user}/")
    @Operation(description = "截获principle resource请求")
    public Response checkAddressBookPrincipleResource(@PathParam("user") String user,
                                                      @HeaderParam("Authorization") String security) {
        if (Objects.isNull(security)) {
            return Response.status(401).header("WWW-Authenticate", "Basic realm=\"Addressbook -Password Required\"").build();
        }  else {
            var userPrinciple = authService.checkAuthResult(security);
            if(Objects.isNull(userPrinciple)) {
                return Response.status(Response.Status.FORBIDDEN).entity(BAD_AUTH).build();
            }
            return Response.status(200).
                    header("Allow", "DELETE, GET, HEAD, MERGE, MKCALENDAR, MKCOL, MOVE, OPTIONS, POST, PROPFIND, PROPPATCH, PUT, REPORT").
                    header("DAV","1, 2, 3, addressbook, extended-mkcol").
                    header("content-length", 0).
                    header("Server",SERVER).
                    build();
        }
    }

    @PUT
    @Path("/{user}/{collectId}/{vcfile}")
    @Consumes("text/vcard")
    public Response putVcfFile(@HeaderParam("If-None-Match") String ifNoneMatch,
                               @HeaderParam("If-Match") String ifMatch,
                               @PathParam("user") String user,
                               @PathParam("collectId") String collectId, @PathParam("vcfile") String vcFile, String body) {
        var value= carddavPropService.checkVcf(ifNoneMatch, ifMatch, user, collectId, vcFile, body);
        var status = Integer.valueOf(value.get(0));
        Response.ResponseBuilder builder = Response.status(status).header("Server", SERVER);
        if(value.size() >= 2 && !StringUtil.isNullOrEmpty(value.get(1))) {
            builder.header("Etag", "\""+value.get(1) + "\"");
        }
        if(value.size() >= 3) {
            builder.entity(value.get(2));
        }
        return builder.build();
    }

    @REPORT
    @Path("/{user}/{collectId}/")
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_HTML})
    @Produces(MediaType.TEXT_HTML)
    public Response reportVcfFile(@HeaderParam("Depth") Integer depth,
                                  @PathParam("user") String user,
                                  @PathParam("collectId") String collectId, String body) {
        var doc = Jsoup.parse(body);
        var document = DocumentHelper.createDocument();
        if(Objects.isNull(user) || Objects.isNull(collectId)) {
            baseService.multistatusGen(document).addElement("supported-report");
            try{
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                XMLWriter writer = new XMLWriter(outputStream);
                writer.write(document);
                writer.close();
                return Response.status(207).header("DAV","1, 2, 3, addressbook, extended-mkcol").
                        header("Server", SERVER).entity(outputStream.toByteArray()).build();
            }catch (Exception ie) {
                return Response.status(403).entity("The method is not allowed on the requested resource.").build();
            }
        }
        var multistatus = baseService.multistatusGen(document);
        var nodesSync = carddavPropService.getReportProps(doc.childNodes(), "sync-collection");
        if(!nodesSync.isEmpty()) {
            rootPropService.getSyncValue(nodesSync, user, collectId, multistatus);
        }
        var nodes = carddavPropService.getReportProps(doc.childNodes(), "card:addressbook-multiget");
        if(!nodes.isEmpty()){
            rootPropService.getAddressMulti(nodes, user, collectId, multistatus);
        }
        try{
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            XMLWriter writer = new XMLWriter(outputStream);
            writer.write(document);
            writer.close();
            return Response.status(207).header("DAV","1, 2, 3, addressbook, extended-mkcol").
                    header("Server", SERVER).entity(outputStream.toByteArray()).build();
        }catch (Exception ie) {
            return Response.status(405).entity("The method is not allowed on the requested resource.").build();
        }
    }

    @DELETE
    @Path("/{user}/{collectId}/{vcfile}")
    @Produces(MediaType.TEXT_HTML)
    public Response deleteVcfFile(@HeaderParam("If-Match") String ifMatch,
                                  @PathParam("user") String user,
                                  @PathParam("collectId") String collectId, @PathParam("vcfile") String vcFile, String body) {
        var code = carddavPropService.deleteVcf(user, collectId, ifMatch,vcFile);
        if(code.equals("200")) {
            var document = carddavCollectService.deleteResponseGen("/"+user+"/"+collectId+"/"+vcFile);
            try{
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                XMLWriter writer = new XMLWriter(outputStream);
                writer.write(document);
                writer.close();
                return Response.status(200).header("DAV","1, 2, 3, addressbook, extended-mkcol").
                        header("Server", SERVER).entity(outputStream.toByteArray()).build();
            }catch (Exception ie) {
                return Response.status(405).entity("The method is not allowed on the requested resource.").build();
            }
        } else if (code.equals("412")) {
            return Response.status(Integer.parseInt(code)).entity("").build();
        } else{
            return Response.status(Integer.parseInt(code)).entity("Not found.").build();
        }
    }

	@MKCOL
	@Path("/{user}/{collectId}")
	@Produces(MediaType.TEXT_PLAIN)
    @Consumes({MediaType.TEXT_XML, MediaType.TEXT_HTML})
	public Response createAddressbook(@HeaderParam("Authorization") String security,
                                      @PathParam("user") String user,
									  @PathParam("collectId") String collectId,  String body){
		if (Objects.isNull(security)) {
			return Response.status(401).header("WWW-Authenticate", "Basic realm=\"Addressbook -Password Required\"").build();
		}  else {
            var userPrinciple = authService.checkAuthResult(security);
            if(Objects.isNull(userPrinciple) || !userPrinciple.equals(user)) {
                return Response.status(Response.Status.FORBIDDEN).entity(BAD_AUTH).build();
            }
            var doc = Jsoup.parse(body);
            var collectProps = carddavCollectService.getProps(carddavCollectService.getMkcolNodes(doc.childNodes())) ;
            if(carddavCollectService.mkColCollectCreate(user, collectId,collectProps)) {
                return Response.status(201).build();
            }else{
                return Response.status(405).entity("The method is not allowed on the requested resource.").build();
            }
		}
	}

    @DELETE
    @Path("/{user}/{collectId}/")
    public Response deleteAddressBookCollection(@HeaderParam("Authorization") String security, @PathParam("user") String user,
                                                @PathParam("collectId") String collectId) {
        if (Objects.isNull(security)) {
            return Response.status(401).header("WWW-Authenticate", "Basic realm=\"Addressbook -Password Required\"").build();
        }else {
            var userPrinciple = authService.checkAuthResult(security);
            if(Objects.isNull(userPrinciple) || !userPrinciple.equals(user)) {
                return Response.status(Response.Status.FORBIDDEN).entity(BAD_AUTH).build();
            }
            if(carddavCollectService.deleteCollect(user,collectId)) {
                var document = carddavCollectService.deleteResponseGen("/"+user+"/"+collectId+"/");
                try{
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    XMLWriter writer = new XMLWriter(outputStream);
                    writer.write(document);
                    writer.close();
                    return Response.status(200).entity(outputStream.toByteArray()).build();
                }catch (Exception ie) {
                    return Response.status(405).entity("The method is not allowed on the requested resource.").build();
                }
            }else{
                return Response.status(405).entity("The method is not allowed on the requested resource.").build();
            }
        }
    }

    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/user/{user}/password/{password}")
    public Response createUser(@PathParam("user") String user, @PathParam("password") String  password) {
        if(authService.createUser(user,password)) {
            return Response.status(200).entity("seuccess").build();
        }else{
            return Response.status(403).entity("failed").build();
        }
    }

}
