package com.nixmash.springdata.solr.common;

import com.nixmash.springdata.jpa.model.Post;
import com.nixmash.springdata.jpa.service.PostService;
import com.nixmash.springdata.solr.exceptions.GeoLocationException;
import com.nixmash.springdata.solr.model.Product;
import com.nixmash.springdata.solr.service.PostDocService;
import com.nixmash.springdata.solr.service.ProductService;
import com.nixmash.springdata.solr.utils.SolrUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.List;

@Component
public class SolrUI {

    private static final String SOLR_RECORD_ID = "SOLR1000";

    @Resource
    private Environment environment;

    @Resource
    private ProductService service;

    private final SolrSettings solrSettings;

    final SolrOperations solrOperations;
    final PostService postService;
    final PostDocService postDocService;

    private Page<Product> productListPage;
    private HighlightPage<Product> highlightProductPage;
    private List<Product> productList;
    private FacetPage<Product> facetProductPage;
    private Iterable<Product> productIterable;

    @Autowired
    public SolrUI(SolrOperations solrOperations, PostDocService postDocService, SolrSettings solrSettings, PostService postService) {
        this.solrOperations = solrOperations;
        this.postDocService = postDocService;
        this.solrSettings = solrSettings;
        this.postService = postService;
    }

    // @formatter:off

    private enum DEMO {
        ALL_PRODUCTS,
        NAMED_QUERY,
        UPDATE_RECORD,
        METHOD_NAME_QUERY,
        AVAILABLE_PRODUCTS,
        ALL_RECORDS,
        TEST_RECORDS,
        CRITERIA_SEARCH,
        ANNOTATED_QUERY,
        FACET_ON_AVAILABLE,
        FACET_ON_CATEGORY,
        FACET_ON_NAME,
        SIMPLE_QUERY,
        HIGHLIGHT_SEARCH,
        HIGHLIGHT_SEARCH_CRITERIA,
        BY_LOCATION,
        POPULATE_DATABASE_SINGLE_ENTRIES,
        POPULATE_DATABASE_AS_LIST
    }

    ;

    // @formatter:on

    public void init() {
        DEMO demo = DEMO.POPULATE_DATABASE_AS_LIST;

        String[] profiles = environment.getActiveProfiles();
        if (profiles[0].equals("dev"))
            System.out.println("DEVELOPMENT mode: Embedded SOLR Home: " + solrSettings.getSolrEmbeddedPath());
        else
            System.out.println("PRODUCTION mode: SOLR Server Url: " + solrSettings.getSolrServerUrl());
        System.out.println("Running Solr Function: " + demo.name() + "\n");

        runDemos(demo);
    }

    private void runDemos(DEMO demo) {

        Query query = new SimpleQuery(new SimpleStringCriteria("doctype:post"));
        List<Post> posts = postService.getAllPublishedPosts();

        switch (demo) {

            case POPULATE_DATABASE_SINGLE_ENTRIES:
                solrOperations.delete(query);
                solrOperations.commit();
                System.out.println("Existing posts deleted...");

                for (Post post :
                        posts) {
                    System.out.println(String.format("Entering Post #%s : %s", post.getPostId(), post.getPostTitle()));
                    postDocService.addToIndex(post);
                }
                System.out.println("All posts added to Solr Server at " + solrSettings.getSolrServerUrl());
                break;


            case POPULATE_DATABASE_AS_LIST:
                solrOperations.delete(query);
                solrOperations.commit();
                System.out.println("Existing posts deleted...");
                postDocService.addAllToIndex(posts);
                System.out.println("All posts added to Solr Server at " + solrSettings.getSolrServerUrl());
                break;

            case BY_LOCATION:
                try {
                    productList = service.getProductsByLocation("35.10,-96.102");
                } catch (GeoLocationException e) {
                    e.printStackTrace();
                }
                printProducts(productList);
                break;

            case HIGHLIGHT_SEARCH_CRITERIA:
                highlightProductPage = service.findByHighlightedNameCriteria("canon powershot");
                SolrUtils.processHighlights(highlightProductPage);
                printProducts(highlightProductPage);
                break;

            case HIGHLIGHT_SEARCH:
                String queryString = "Can on yep";
                Boolean matches = queryString.matches("[a-zA-Z_0-9 ]*");
                System.out.println("MATCHES: " + matches);
                //			highlightProductPage = service.findByHighlightedName("canon", new PageRequest(0, 20));
//			highlightProductPage.getContent();
//			SolrUtils.processHighlights(highlightProductPage);
//			printProducts(highlightProductPage);
                break;

            case SIMPLE_QUERY:
                // productList = service.getProductsWithUserQuery("name:memory AND name:corsair) AND
                // popularity:[6 TO *]");
                // productList = service.getProductsWithUserQuery("name:Western+Digital AND inStock:TRUE");
                // productList = service.getProductsWithUserQuery("cat:memory");
                // productList = service.getProductsWithUserQuery("features::printer");
                productList = service.getProductsWithUserQuery("inStock:true");
                printProducts(productList);
                break;

            case FACET_ON_NAME:

                facetProductPage = service.autocompleteNameFragment("pr", new PageRequest(0, 1));
                Page<FacetFieldEntry> fnPage = facetProductPage.getFacetResultPage(Product.NAME_FIELD);

                for (FacetFieldEntry entry : fnPage) {
                    System.out.println(String.format("%s:%s \t %s", entry.getField().getName(), entry.getValue(),
                            entry.getValueCount()));
                }

                break;

            case FACET_ON_AVAILABLE:

                facetProductPage = service.getFacetedProductsAvailable();
                Page<FacetFieldEntry> avPage = facetProductPage.getFacetResultPage(Product.AVAILABLE_FIELD);

                for (FacetFieldEntry entry : avPage) {
                    System.out.println(String.format("%s:%s \t %s", entry.getField().getName(), entry.getValue(),
                            entry.getValueCount()));
                }

                break;

            case FACET_ON_CATEGORY:

                facetProductPage = service.getFacetedProductsCategory();
                Page<FacetFieldEntry> catPage = facetProductPage.getFacetResultPage(Product.CATEGORY_FIELD);

                for (FacetFieldEntry entry : catPage) {
                    System.out.println(String.format("%s:%s \t %s", entry.getField().getName(), entry.getValue(),
                            entry.getValueCount()));
                }

                break;

            case METHOD_NAME_QUERY:

                productList = service.getProductsByStartOfName("power cord");
                printProducts(productList);
                break;

            case ANNOTATED_QUERY:

                productList = service.getProductsByNameOrCategoryAnnotatedQuery("canon");
                printProducts(productList);
                break;

            case NAMED_QUERY:

                productIterable = service.getProductsByNameOrCategory("canon");
                printProducts(productIterable);
                break;

            case TEST_RECORDS:

                productListPage = service.getTestRecords();
                printProducts(productListPage);
                break;

            case AVAILABLE_PRODUCTS:
                productList = service.getAvailableProducts();
                printProducts(productList);
                break;

            case ALL_PRODUCTS:

                productList = service.getProductsByFilter();
                printProducts(productList);

                productList = service.getProducts();
                printProducts(productList);

                break;

            case ALL_RECORDS:

                Iterable<Product> allRecords = service.getAllRecords();
                printProducts(allRecords);
                break;

            case UPDATE_RECORD:

                Product urProduct = service.getProduct(SOLR_RECORD_ID);
                System.out.println(String.format("Original Product Name: %s", urProduct.getName()));
                urProduct.setName("Solr, The Enterprisey Http Search Server");
                service.updateProductName(urProduct);

                Product urProductUpdated = service.getProduct(SOLR_RECORD_ID);
                System.out.println(String.format("New Product Name: %s", urProductUpdated.getName()));

                urProductUpdated.setName("Solr, The Enterprise Http Search Server");
                service.updateProductName(urProductUpdated);

                break;

            case CRITERIA_SEARCH:

                productList = service.searchWithCriteria("Canon Camera memory");
                printProducts(productList);

                break;

            default:
                break;
        }

    }


    private void printProducts(Iterable<? extends Product> products) {
        int i = 0;
        System.out.println("");
        for (Product product : products) {
            MessageFormat mf = new MessageFormat("{0} | Popularity: {1} | Lng/Lat: {2},{3}");
            Object[] items = {product.getName(), product.getPopularity(), product.getPoint().getX(),
                    product.getPoint().getY()};

            if (product.getPoint().getX() < 0) {
                mf = new MessageFormat("{0} | Popularity: {1}");
            }

            System.out.println(mf.format(items));
            i++;
        }
        System.out.println("\nTOTAL RECORDS: " + i);
    }

}
