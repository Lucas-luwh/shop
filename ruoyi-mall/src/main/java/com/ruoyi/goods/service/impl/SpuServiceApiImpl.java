package com.ruoyi.goods.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.utils.IteratorUtils;
import com.ruoyi.common.utils.WeChatAppletUtils;
import com.ruoyi.common.utils.bean.WeChatAppletCodeRequest;
import com.ruoyi.common.utils.bean.WechatSetting;
import com.ruoyi.goods.domain.*;
import com.ruoyi.goods.service.*;
import com.ruoyi.goods.vo.CombinationDetail;
import com.ruoyi.goods.vo.SpuDetail;
import com.ruoyi.goods.vo.SpuDetailItem;
import com.ruoyi.marketing.domain.GoodsCombination;
import com.ruoyi.marketing.domain.Marketing;
import com.ruoyi.marketing.domain.MarketingItem;
import com.ruoyi.marketing.domain.MarketingSetting;
import com.ruoyi.marketing.service.*;
import com.ruoyi.marketing.vo.CrowdFundingSpuDetail;
import com.ruoyi.member.domain.UmsMember;
import com.ruoyi.member.service.IUmsMemberService;
import com.ruoyi.member.service.WxAppletAccessTokenService;
import com.ruoyi.order.domain.OmsLogisticsTemplate;
import com.ruoyi.order.service.IOmsLogisticsTemplateService;
import com.ruoyi.order.service.IOmsOrderService;
import com.ruoyi.order.vo.SkuMarketPriceDetail;
import com.ruoyi.setting.bean.WechatPaySet;
import com.ruoyi.setting.service.BaseInfoSetService;
import com.ruoyi.setting.service.ILsPaySettingService;
import com.ruoyi.store.domain.TStoreInfo;
import com.ruoyi.store.service.ITStoreCommentService;
import com.ruoyi.store.service.ITStoreInfoService;
import com.ruoyi.store.service.StoreInfoServiceApi;
import com.ruoyi.store.vo.StoreItem;
import com.ruoyi.util.CalcFreightUtil;
import com.ruoyi.util.CommonConstant;
import org.apache.commons.lang.ArrayUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import sun.misc.BASE64Decoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by ??????????????? on 17/11/22.
 * ????????????????????????
 */
@Service
public class SpuServiceApiImpl implements SpuServiceApi {

    /**
     * ????????????
     */
    private Logger logger = LoggerFactory.getLogger(SpuServiceApiImpl.class);

    /**
     * ????????????????????????
     */
    @Autowired
    private IPmsGoodsService spuService;

    /**
     * ????????????????????????
     */
    @Autowired
    private StoreInfoServiceApi storeInfoServiceApi;

    /**
     * ??????????????????
     */
    @Autowired
    private IPmsSkuService skuService;

    /**
     * ????????????????????????
     */
    @Autowired
    private IPmsGoodsServiceSupportService spuServicceSupportService;

    /**
     * ?????????????????????????????????
     */
    @Autowired
    private IPmsGoodsAttributeValueService spuAttributeValueService;

    /**
     * ????????????????????????
     */
    @Autowired
    private MarketingQueryService marketingQueryService;

    /**
     * ????????????????????????
     */
    @Autowired
    private ITStoreInfoService storeInfoService;


    /**
     * ??????????????????????????????
     */
    @Autowired
    private CrowdfundingProgressService crowdfundingProgressService;

    /**
     * ????????????????????????????????????
     */
    @Autowired
    private IPmsSkuMemberPriceService skuMemberPriceServicce;

    /**
     * ????????????????????????
     */
    @Autowired
    private IUmsMemberService customerService;

    /**
     * ???????????????
     */
    @Autowired
    private CouponService couponService;

    /**
     * ????????????????????????
     */
    @Autowired
    private IPmsSpecService specService;

    /**
     * ????????????service
     */
    @Autowired
    private IPmsCommentService commentService;

    /**
     * ????????????????????????
     */
    @Autowired
    private IPmsAttentionService attentionService;

    /**
     * ????????????????????????
     */
    @Autowired
    private IPmsCategoryService categoryService;

    /**
     * ??????????????????????????????
     */
    @Autowired
    private GoodsCombinationService goodsCombinationService;

    /**
     * ??????????????????????????????
     */
    @Autowired
    private ITStoreCommentService storeCommentService;

    /**
     * ????????????????????????
     */
    @Autowired
    private IPmsBrandService brandService;

    /**
     * ????????????????????????
     */
    @Autowired
    private BaseInfoSetService baseInfoSetService;

    /**
     * ????????????????????????
     */
    @Autowired
    private IOmsLogisticsTemplateService logisticsTemplateService;

    /**
     * ????????????????????????
     */
    @Autowired
    private MarketingSettingService marketingSettingService;

    /**
     * ??????????????????
     */
    @Autowired
    private IOmsOrderService orderService;

    /**
     * ????????????????????????
     */
    @Autowired
    private ILsPaySettingService paySetService;


    /**
     * ???????????????????????????
     */
    // @Autowired
    // private OssService ossService;

    /**
     * ?????????????????????access_token????????????
     */
    @Autowired
    private WxAppletAccessTokenService wxAppletAccessTokenService;


    @Override
    public Optional<SpuDetail> queryGoodsDetail(Long goodsId, long customerId, SpuDetailItem... spuDetailItems) {
        logger.debug("querySpuDetail and goodsId:{} \r\n customerId:{} \r\n spuDetailItems:{}", goodsId, customerId, spuDetailItems);

        // ????????????
        PmsGoods spu = spuService.querySimpleSpu(goodsId, CommonConstant.QUERY_WITH_NO_STORE);
        if (spu==null || spu.getId()==null) {
            logger.error("querySpuDetail fail due to spu validate fail...spu:{}", spu);
            return Optional.empty();
        }
        // ?????????????????????
        PmsSku sku = null;

        List<PmsSku> skuList = skuService.querySkuBySpuId(spu.getId(), spu.getStoreId());
        if (skuList != null && skuList.size() > 0) {
            sku = skuList.get(0);
        }
        // ????????????
        if (!validateSku(sku)) {
            logger.error("querySpuDetail fail due to sku validate fail...sku:{}", sku);
            return Optional.empty();
        }

        // ??????????????????
        skuService.setSkuDetail(sku, PmsSkuItem.IMAGE, PmsSkuItem.SPEC, PmsSkuItem.BATCH);

        // ????????????
        SpuDetail spuDetail = SpuDetail.build(Optional.of(sku)).addSkuImages(sku.getSkuImages()).addStoreInfo(storeInfoServiceApi.queryStoreInfo(sku.getStoreId(), StoreItem.ATTENNUM)).
                addServiceSupports(spuServicceSupportService.queryBySpuId(sku.getSpuId(), SpuServiceSupportItem.SERVICE_SUPPORT)).addMarketPrice(this.calcMarketingPrice(sku, sku.getPrice(), customerId)).
                addSkuSpecValues(sku.getSkuSpecValues()).addSpuAttributeValues(spuAttributeValueService.queryBySpuId(sku.getSpuId())).addTypeId(spu).addVideoInfo(spu);

        spuDetail.setMarketings(marketingQueryService.queryMarketingsBySkuId(sku.getId(), true, MarketingItem.FULL_DOWN_MARKETING, MarketingItem.FULL_DISCOUNT_MARKETING, MarketingItem.FULL_GIFT_MARKETING, MarketingItem.DEPOSIT_PRE_SALE_MARKETING, MarketingItem.FULL_PRE_SALE_MARKETING, MarketingItem.GROUP_MARKETING,MarketingItem.FALL_MARKETING));
        spuDetail.setSkuList(skuList);
        //??????????????????
        spuDetail.setSku(sku);

        //??????????????????
        MarketingSetting marketingSetting = marketingSettingService.queryMarketingSetting();
        if (Objects.nonNull(marketingSetting)) {
            spuDetail.setPreSaleRule(marketingSetting.getPreSaleRule());
        }

        // ????????????????????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.COUPON)) {
            spuDetail.addCoupons(couponService.queryCouponForSpu(sku.getStoreId(), true, 1));
        }

        // ?????????????????????????????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.SPU_SPECS)) {
            spuDetail.setSpecs(specService.querySpuSpecs(spuDetail.getSpuId()));
        }

        // ????????????????????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.SKU_COMMENT_NUM)) {
            spuDetail.setSkuCommentNum(commentService.queryCommentCountBySkuId(sku.getId()));
        }

        // ?????????????????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.FOLLOW)) {
            spuDetail.setHasAtten(attentionService.hasAttention(customerId, sku.getId()));
        }

        // ???????????????????????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.CATE)) {
            spuDetail.setCategories(categoryService.queryAllParentCategoryById(spu.getThirdCateId()));
        }

        // ??????????????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.STORE_SCORE)) {
            spuDetail.setStoreScore(storeCommentService.queryStoreScoreWithStoreInfo(spu.getStoreId()));
        }

        // ??????????????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.BRAND)) {
            spuDetail.setBrand(brandService.queryBrandById(spu.getBrandId(), CommonConstant.QUERY_WITH_NO_STORE));
        }

        // ??????pc?????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.PC_DESC)) {
            spuDetail.setPcDesc(spu.getPcDesc());
        }

        // ??????mobile?????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.MOBILE_DESC)) {
            spuDetail.setMobileDesc(spu.getMobileDesc());
        }

        return Optional.of(spuDetail);
    }

    @Override
    public Optional<SpuDetail> querySpuDetail(String skuId, long customerId, SpuDetailItem... spuDetailItems) {
        logger.debug("querySpuDetail and skuId:{} \r\n customerId:{} \r\n spuDetailItems:{}", skuId, customerId, spuDetailItems);

        // ?????????????????????
        PmsSku sku = skuService.querySkuById(skuId);

        // ????????????
        if (!validateSku(sku)) {
            logger.error("querySpuDetail fail due to sku validate fail...sku:{}", sku);
            return Optional.empty();
        }

        // ??????????????????
        skuService.setSkuDetail(sku, PmsSkuItem.IMAGE, PmsSkuItem.SPEC, PmsSkuItem.BATCH);

        // ????????????
        PmsGoods spu = spuService.querySimpleSpu(sku.getSpuId(), CommonConstant.QUERY_WITH_NO_STORE);

        // ????????????
        SpuDetail spuDetail = SpuDetail.build(Optional.of(sku)).addMarketings(marketingQueryService.queryMarketingsBySkuId(skuId, true, MarketingItem.FULL_DOWN_MARKETING, MarketingItem.FULL_DISCOUNT_MARKETING, MarketingItem.FULL_GIFT_MARKETING, MarketingItem.DEPOSIT_PRE_SALE_MARKETING, MarketingItem.FULL_PRE_SALE_MARKETING, MarketingItem.GROUP_MARKETING)).addSkuImages(sku.getSkuImages()).addStoreInfo(storeInfoServiceApi.queryStoreInfo(sku.getStoreId(), StoreItem.ATTENNUM)).
                addServiceSupports(spuServicceSupportService.queryBySpuId(sku.getSpuId(), SpuServiceSupportItem.SERVICE_SUPPORT)).addMarketPrice(this.calcMarketingPrice(sku, sku.getPrice(), customerId)).
                addSkuSpecValues(sku.getSkuSpecValues()).addSpuAttributeValues(spuAttributeValueService.queryBySpuId(sku.getSpuId())).addTypeId(spu).addVideoInfo(spu);

        //??????????????????
        spuDetail.setSku(sku);

        //??????????????????
        MarketingSetting marketingSetting = marketingSettingService.queryMarketingSetting();
        if (Objects.nonNull(marketingSetting)) {
            spuDetail.setPreSaleRule(marketingSetting.getPreSaleRule());
        }

        // ????????????????????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.COUPON)) {
            spuDetail.addCoupons(couponService.queryCouponForSpu(sku.getStoreId(), true, 1));
        }

        // ?????????????????????????????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.SPU_SPECS)) {
            spuDetail.setSpecs(specService.querySpuSpecs(spuDetail.getSpuId()));
        }

        // ????????????????????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.SKU_COMMENT_NUM)) {
            spuDetail.setSkuCommentNum(commentService.queryCommentCountBySkuId(skuId));
        }

        // ?????????????????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.FOLLOW)) {
            spuDetail.setHasAtten(attentionService.hasAttention(customerId, skuId));
        }

        // ???????????????????????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.CATE)) {
            spuDetail.setCategories(categoryService.queryAllParentCategoryById(spu.getThirdCateId()));
        }

        // ??????????????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.STORE_SCORE)) {
            spuDetail.setStoreScore(storeCommentService.queryStoreScoreWithStoreInfo(spu.getStoreId()));
        }

        // ??????????????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.BRAND)) {
            spuDetail.setBrand(brandService.queryBrandById(spu.getBrandId(), CommonConstant.QUERY_WITH_NO_STORE));
        }

        // ??????pc?????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.PC_DESC)) {
            spuDetail.setPcDesc(spu.getPcDesc());
        }

        // ??????mobile?????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.MOBILE_DESC)) {
            spuDetail.setMobileDesc(spu.getMobileDesc());
        }

        return Optional.of(spuDetail);
    }

    @Override
    public CrowdFundingSpuDetail queryCrowdFundingSpuDetail(long marketingId, String skuId, boolean isNeedMarketingDetail, SpuDetailItem... spuDetailItems) {
        logger.debug("queryCrowdFundingSpuDetail and marketingId:{} \r\n skuId:{} \r\n isNeedMarketingDetail:{} \r\n spuDetailItems:{}", marketingId, skuId, isNeedMarketingDetail, spuDetailItems);
        Marketing marketing = marketingQueryService.queryMarketingById(marketingId, CommonConstant.QUERY_WITH_NO_STORE);
        if (Objects.isNull(marketing)) {
            logger.error("queryCrowdFundingSpuDetail fail: no marketing");
            return null;
        }
        if (marketing.getStartTime().isAfter(LocalDateTime.now())) {
            logger.error("queryCrowdFundingSpuDetail fail:  marketing is not start");
            return null;
        }
        if (CollectionUtils.isEmpty(marketing.getMarketingSkus())) {
            logger.error("queryCrowdFundingSpuDetail fail : no marketing sku");
            return null;
        }
        if (StringUtils.isEmpty(skuId)) {
            skuId = marketing.getMarketingSkus().get(0).getSkuId();
        }
        PmsSku sku = skuService.querySkuById(skuId);
        // ??????????????????
        skuService.setSkuDetail(sku, PmsSkuItem.IMAGE, PmsSkuItem.SPEC);
        if (Objects.isNull(sku)) {
            logger.error("queryCrowdFundingSpuDetail fail : no sku");
            return null;
        }
        sku.setPrice(marketing.getCrowdFundingSkuPrice(skuId));
        // ????????????
        PmsGoods spu = spuService.querySimpleSpu(sku.getSpuId(), CommonConstant.QUERY_WITH_NO_STORE);

        CrowdFundingSpuDetail crowdFundingSpuDetail = CrowdFundingSpuDetail.build(sku).addServiceSupports(spuServicceSupportService.queryBySpuId(sku.getSpuId(), SpuServiceSupportItem.SERVICE_SUPPORT))
                .addSpuAttributeValues(spuAttributeValueService.queryBySpuId(sku.getSpuId())).addCategories(categoryService.queryAllParentCategoryById(spu.getThirdCateId()))
                .addStoreName(storeInfoService.queryStoreInfo(sku.getStoreId()));

        // ?????????????????????????????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.SPU_SPECS)) {
            crowdFundingSpuDetail.setSpecs(specService.querySpuSpecs(crowdFundingSpuDetail.getSpuId()));
        }
        // ??????pc?????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.PC_DESC)) {
            crowdFundingSpuDetail.setPcDesc(spu.getPcDesc());
        }
        // ??????mobile?????????
        if (ArrayUtils.contains(spuDetailItems, SpuDetailItem.MOBILE_DESC)) {
            crowdFundingSpuDetail.setMobileDesc(spu.getMobileDesc());
        }
        //????????????????????????
        if (isNeedMarketingDetail) {
            crowdFundingSpuDetail.buildForSite(marketing,
                    crowdfundingProgressService.queryCrowdfundingProgressByMarketingId(marketingId, CommonConstant.QUERY_WITH_NO_STORE),
                    marketingSettingService.queryMarketingSetting(), orderService.queryCrowFundingCustomerCount(marketingId, CommonConstant.QUERY_WITH_NO_STORE));
        }
        return crowdFundingSpuDetail;

    }


    /**
     * ?????????????????????(????????????,?????????????????????)
     * <p>
     * ?????? ??????>??????>????????????
     * ??????????????????????????????????????? ?????????????????????
     *
     * @param sku        ??????
     * @param price      ??????
     * @param customerId ??????id
     * @return ??????????????????
     */
    @Override
    public SkuMarketPriceDetail calcMarketingPrice(PmsSku sku, BigDecimal price, long customerId) {

       // logger.debug("calcMarketingPrice and sku:{} \r\n and price:{} \r\n and customerId:{}", sku, price, customerId);

        // ?????????????????????????????? ???????????????
        if (Objects.isNull(price) || StringUtils.isEmpty(sku)) {
            logger.error("calcMarketingPrice fail due to params is error..");
            return new SkuMarketPriceDetail();
        }
        String skuId = sku.getId();
        // ????????????
        SkuMarketPriceDetail skuMarketPriceDetail = SkuMarketPriceDetail.build(price);

        // ????????????????????? ??????,??????,??????
        List<Marketing> marketings = marketingQueryService.queryMarketingsBySkuId(skuId, true, MarketingItem.FALL_MARKETING, MarketingItem.PANIC_BUY_MARKETING, MarketingItem.DEPOSIT_PRE_SALE_MARKETING, MarketingItem.FULL_PRE_SALE_MARKETING);

        // ????????????????????????????????? ??????????????????????????????(?????????????????????????????? ???????????????)
        if (CollectionUtils.isEmpty(marketings)) {
            // ??????????????????
            return this.calcMemberPrice(skuId, price, customerId);
        }

        // ?????????????????? ??????????????????
        Optional<Marketing> marketing = IteratorUtils.filterMatch(marketings, Marketing::isDepositPreSaleMarketing);

        // ??????????????????
        if (!marketing.isPresent()) {
            marketing = IteratorUtils.filterMatch(marketings, Marketing::isFullPreSaleMarkting);
        }

        // ????????????
        if (!marketing.isPresent()) {
            marketing = IteratorUtils.filterMatch(marketings, Marketing::isPanicBuyMarketing);
        }

        // ???????????????
        if (!marketing.isPresent()) {
            marketing = IteratorUtils.filterMatch(marketings, Marketing::isFallMarketing);
        }

        // ??????????????????????????????
        if (!marketing.isPresent()) {
            return skuMarketPriceDetail;
        }

        //??????????????????????????????
        return skuMarketPriceDetail.setPriceToMarketPrice(marketing.get(), sku);

    }

    @Override
    public SkuMarketPriceDetail calcMemberPrice(String skuId, BigDecimal price, long customerId) {

        logger.debug("calcMemberPrice and skuId:{} \r\n price:{} \r\n customerId:{}", skuId, price, customerId);

        SkuMarketPriceDetail skuMarketPriceDetail = SkuMarketPriceDetail.build(price);

        // ?????????????????????????????? ???????????????
        if (!baseInfoSetService.queryBaseInfoSet().isMemberPriceOpen()) {
            logger.info("MemberPrice is not open...");
            return skuMarketPriceDetail;
        }

        // ??????????????????
        PmsSkuMemberPrice skuMemberPrice = getSkuMemberPrice(skuId, customerId);

        // ??????????????????????????? ???????????????????????????
        if (Objects.nonNull(skuMemberPrice)) {
            // ??????????????????????????????
            return skuMarketPriceDetail.setPriceToMemberPrice(skuMemberPrice.getPrice());
        }

        return skuMarketPriceDetail;
    }

    @Override
    public CombinationDetail queryGoodsCombinationBySkuId(String skuId, long customerId) {
        logger.debug("queryGoodsCombinationBySkuId and skuId:{},customerId{}", skuId, customerId);

        if (StringUtils.isEmpty(skuId)) {
            logger.error("queryGoodsCombinationBySkuId fail due to skuId is empty....");
            return CombinationDetail.buildNoCombination();
        }

        // ????????????
        GoodsCombination goodsCombination = goodsCombinationService.querySkusBySku(skuId);

        //??????????????????????????? ???????????????
        if (Objects.isNull(goodsCombination) || CollectionUtils.isEmpty(goodsCombination.getSkuIds())) {
            return CombinationDetail.buildNoCombination();
        }

        // ????????????  ?????????????????????
        List<String> skuIds = goodsCombination.getSkuIds().stream().filter(s -> !s.equals(skuId)).collect(Collectors.toList());

        logger.debug("begin to get spudetail and skuIds:{}", skuIds);

        return CombinationDetail.buildHasCombination(goodsCombination, skuIds.parallelStream().map(id -> this.queryCombinationSpuDetail(id, customerId)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
    }

    @Override
    public List<SpuDetail> querySpuDetailList(String[] skuIds, SpuDetailItem... spuDetailItems) {
        logger.debug("querySpuDetailList and skuIds:{} \r\n ", Arrays.toString(skuIds));
        if (ArrayUtils.isEmpty(skuIds)) {
            logger.error("querySpuDetailList fail :skuIds is empty");
            return Collections.emptyList();
        }
        List<SpuDetail> finalList = new ArrayList<>();
        Arrays.asList(skuIds).forEach(s ->
                finalList.add(this.querySpuDetail(s, CommonConstant.NO_CUSTOMER_ID, spuDetailItems).orElse(null))
        );
        return finalList;
    }

    @Override
    public int updateSpu(PmsGoods spu, Consumer<Long> consumer) {
        if (Objects.isNull(spu)) {
            logger.error("updateSpu fail due to goods is null....");
            return 0;
        }
        if (spu.getSkus().stream().anyMatch(PmsSku::hasBatchPriceAndMemberPrice)) {
            logger.error("updateSpu fail due to exist sku has member price and batch both");
            return -3;
        }
        if (marketingQueryService.queryExclusionMarketingCountBySkuIds(spu.getHasMemberPriceSkuIds(), CommonConstant.MEMBER_PRICE_EXCLUSION) > 0) {
            logger.error("updateSpu fail: member price has exclusion marketing");
            return -1;
        }
        if (marketingQueryService.queryExclusionMarketingCountBySkuIds(spu.getHasBatchSkuIds(), CommonConstant.BATCH_SKU_EXCLUSION) > 0) {
            logger.error("updateSpu fail: batch sku has exclusion marketing");
            return -2;
        }
        return spuService.updateSpu(spu, consumer);
    }

    @Override
    public BigDecimal calculateFreight(String skuId, long storeId, long cityId, int num) {
        logger.debug("calculateFreight and skuId:{} \r\n storeId:{} \r\n cityId:{} \r\n num:{} ", skuId, storeId, cityId, num);

        // ??????????????????
        PmsSku sku = skuService.querySkuById(skuId);

        // ???????????????????????????
        if (Objects.isNull(sku) || sku.isVirtualSku()) {
            logger.error("calculateFreight fail : no sku or sku is virtual");
            return new BigDecimal(0L);
        }

        // ???????????????????????????
        List<OmsLogisticsTemplate> logisticsTemplates = logisticsTemplateService.queryLogisticsTemplateByCityIdAndId(new HashSet<>(Arrays.asList(sku.getLogisticsTemplateId())), cityId);

        if (CollectionUtils.isEmpty(logisticsTemplates)) {
            logger.error("calculateFreight fail due to no logisticsTemplates");
            return BigDecimal.ZERO;
        }

        return CalcFreightUtil.calcFreightPrice(logisticsTemplates.get(0), num, sku.getWeight());
    }

    @Override
    public Void exportCheckedSpu(OutputStream os, Long[] ids, long storeId) {
        logger.debug("exportCheckedSpu and ids:{} \r\n storeId:{}", ids, storeId);
        if (ArrayUtils.isEmpty(ids)) {
            logger.error("exportCheckedOrder fail :ids is empty");
            return null;
        }
        exportSpu(fillExportSpuOtherInfo(spuService.querySpuByIdsForExport(Arrays.asList(ids), storeId)), os);
        return null;
    }

    @Override
    public Void exportAllSpu(OutputStream os, long storeId) {
        logger.debug("exportAllSpu and storeId:{}", storeId);
        exportSpu(fillExportSpuOtherInfo(spuService.queryAllSpuForExport(storeId)), os);
        return null;
    }

    @Override
    public String getWeChatAppletCode(WeChatAppletCodeRequest weChatAppletCodeRequest) {
        logger.debug("getWeChatAppletCode and weChatAppletCodeRequest:{} ", weChatAppletCodeRequest);
        if (Objects.isNull(weChatAppletCodeRequest)) {
            return null;
        }
        ByteArrayInputStream inputStream = null;
        String imageUrl = "";
        try {
            // ?????????????????????????????????
            WechatPaySet wechatAppletPaySet = paySetService.queryPaySet().getWechatAppletPaySet();
            WechatSetting wechatSetting = new WechatSetting();
            wechatSetting.setAppId(wechatAppletPaySet.getAppId());
            wechatSetting.setAppSecret(wechatAppletPaySet.getAppSecret());
            // ????????????????????????????????????????????????
            String accessToken = wxAppletAccessTokenService.getWxAppletAccessToken(wechatSetting);
            if (StringUtils.isEmpty(accessToken)) {
                logger.error("getWeChatAppletCode fail due to accessToken is null");
                return null;
            }
            // ??????????????????????????????????????????
            String getShareAppletCodeUrl = WeChatAppletUtils.getWeChatAppletShareCodeUrl(accessToken);
            if (StringUtils.isEmpty(getShareAppletCodeUrl)) {
                logger.error("getWeChatAppletCode fail due to getShareAppletCodeUrl is null");
                return null;
            }
            inputStream = WeChatAppletUtils.getJsonRequestResult(getShareAppletCodeUrl, JSON.toJSONString(weChatAppletCodeRequest));

            //????????????????????????????????????????????????????????????????????????????????????200
            if (inputStream.available() <= 200) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                int i;
                byte[] buffer = new byte[200];
                while ((i = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, i);
                }
                String str = new String(byteArrayOutputStream.toByteArray());
                JSONObject jsonObject = JSONObject.parseObject(str);
                byteArrayOutputStream.close();
                if (!StringUtils.isEmpty(jsonObject.getString("errcode"))) {
                    if (!StringUtils.isEmpty(jsonObject.getString("errmsg"))) {
                        logger.error("getWeChatAppletCode Fail and errmsg:{}", jsonObject.getString("errmsg"));
                    }
                    return null;
                }
            }
            imageUrl = uploadToOssForBase64(WeChatAppletUtils.getBase64FromInputStream(inputStream));
            //  imageUrl = uploadToUpYunForBase64(WeChatAppletUtils.getBase64FromInputStream(inputStream));
            //??????????????????????????????oss????????????????????????????????????
//                String fileName = "noilCode_userNo" + ".jpeg";
//                String path = "wxcode/noilCode";
//                String imgUrl = ossClientUtil.UploadImgAndReturnImgUrlInputStream(inputStream, fileName, path);
            //????????????????????????
//            FileOutputStream fileOutputStream = new FileOutputStream("D:/123.png");
//            int i;
//            byte[] buffer = new byte[200];
//            while ((i = inputStream.read(buffer)) != -1) {
//                fileOutputStream.write(buffer, 0, i);
//            }
//            fileOutputStream.flush();
//            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        return imageUrl;
    }


    /**
     * ??????base64??????
     *
     * @return ?????????????????????????????????
     * @throws Exception
     */
    private String uploadToOssForBase64(String image) throws Exception {
        if (StringUtils.isEmpty(image)) {
            return null;
        }
        // base64?????????????????????
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] date_blob = decoder.decodeBuffer(image);
        for (int i = 0; i < date_blob.length; ++i) {
            if (date_blob[i] < 0) {
                date_blob[i] += 256;
            }
        }
        return null;
//        return ossService.uploadToQqOssForBase64(Arrays.asList(UploadData.build(null, date_blob, null, "0",null))).stream().findFirst().orElse("");
    }

    /**
     * ?????????????????????????????????
     *
     * @param skuId      ??????id
     * @param customerId ??????id
     * @return ??????????????????
     */
    private Optional<SpuDetail> queryCombinationSpuDetail(String skuId, long customerId) {
        logger.debug("queryCombinationSpuDetail and skuId:{} , customerId:{}", skuId, customerId);

        // ?????????????????????
        PmsSku sku = skuService.querySkuById(skuId);

        // ????????????
        if (!validateSku(sku)) {
            logger.error("querySpuDetail fail due to sku validate fail...sku:{}", sku);
            return Optional.empty();
        }
        skuService.setSkuDetail(sku, PmsSkuItem.BATCH);
        return Optional.of(SpuDetail.build(Optional.of(sku)).addMarketPrice((this.calcMarketingPrice(sku, sku.getPrice(), customerId))).addSkuImages(Arrays.asList(PmsSkuImage.build(sku))));
    }

    /**
     * ???????????????????????????
     *
     * @param skuId      ??????id
     * @param customerId ??????id
     * @return ???????????????????????????
     */
    private PmsSkuMemberPrice getSkuMemberPrice(String skuId, long customerId) {

        // ??????????????????
        UmsMember customer = customerService.queryCustomerWithCustomerLevel(customerId);

        if (Objects.isNull(customer)) {
            return null;
        }

        // ???????????????????????????
        List<PmsSkuMemberPrice> skuMemberPrices = skuMemberPriceServicce.queryBySkuId(skuId);

        if (CollectionUtils.isEmpty(skuMemberPrices)) {
            return null;
        }

        return skuMemberPrices.stream().filter(skuMemberPrice -> skuMemberPrice.getMemberLevelId() == customer.getCustomerLevelId()).findFirst().orElse(null);
    }


    /**
     * ??????????????????
     *
     * @param sku ????????????
     * @return ???????????? 1 ?????????????????? 2 ????????????????????????  3 ??????????????????????????? 4 ???????????????????????????
     */
    private boolean validateSku(final PmsSku sku) {
        if (Objects.isNull(sku)) {
            return false;
        }
        //????????????????????????
        if (!storeInfoService.isEffective(sku.getStoreId())) {
            logger.error("validateSku fail : store is not effective");
            return false;
        }
        return sku.validate(false);
    }


    /**
     * ??????????????????
     *
     * @param spus ??????????????????
     * @param os   ?????????
     */
    private void exportSpu(final List<PmsGoods> spus, final OutputStream os) {
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet("????????????");
        // ??????excel???????????????
        createExecleBase(wb, sheet);
        // ???????????????????????????
        createExecleData(sheet, spus, 1);
        try {
            // ???????????????
            wb.write(os);
        } catch (IOException e) {
            logger.error("export goods fail", e);
        }
    }

    /**
     * ??????excel???????????????
     *
     * @param wb    excel??????
     * @param sheet excel??????sheet??????
     */
    private void createExecleBase(final HSSFWorkbook wb, final HSSFSheet sheet) {
        HSSFRow row = sheet.createRow(0);
        HSSFCellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        List<Integer> widthList = Arrays.asList(6000, 6000, 6000, 6000, 6000, 10000, 6000, 6000, 6000, 6000);
        List<String> cellNameList = Arrays.asList("SPU??????", "SKU??????", "??????", "????????????", "??????", "?????????", "??????", "??????", "??????", "????????????");
        HSSFCell temp;
        for (int i = 0; i < widthList.size(); i++) {
            // ????????????
            sheet.setColumnWidth(i, widthList.get(i));
            // ??????????????????
            temp = row.createCell(i);
            temp.setCellStyle(style);
            temp.setCellValue(cellNameList.get(i));
        }
    }

    /**
     * ????????????execel?????????
     *
     * @param sheet  excel??????sheet??????
     * @param spus   ??????????????????
     * @param offset ??????????????????????????????
     */
    private void createExecleData(final HSSFSheet sheet, final List<PmsGoods> spus, final int offset) {
        if (CollectionUtils.isEmpty(spus)) {
            return;
        }
        final StringBuilder skip = new StringBuilder("0");
        IntStream.range(0, spus.size()).forEach(index -> {
            HSSFRow row = sheet.createRow(offset + index + Integer.parseInt(skip.toString()));
            PmsGoods spu = spus.get(index);
            if (!StringUtils.isEmpty(spu.getName())) {
                row.createCell(0).setCellValue(spu.getName());
            }
            if (CollectionUtils.isEmpty(spu.getSkus())) {
                return;
            }
            IntStream.range(0, spu.getSkus().size()).forEach(index2 -> {
                HSSFRow tempRow = row;
                if (index2 != 0) {
                    tempRow = sheet.createRow(offset + index + Integer.parseInt(skip.toString()) + index2);
                }
                PmsSku sku = spu.getSkus().get(index2);
                if (!StringUtils.isEmpty(sku.getName())) {
                    tempRow.createCell(1).setCellValue(sku.getName());
                }
                if (!CollectionUtils.isEmpty(sku.getSkuSpecValues())) {
                    tempRow.createCell(2).setCellValue(sku.getSpecValuesString());
                }
                if (!StringUtils.isEmpty(sku.getIsVirtual())) {
                    tempRow.createCell(3).setCellValue("1".equals(sku.getIsVirtual()) ? "???" : "???");
                }
                if (!StringUtils.isEmpty(sku.getShelvesStatus())) {
                    tempRow.createCell(4).setCellValue("0".equals(sku.getShelvesStatus()) ? "??????" : "1".equals(sku.getShelvesStatus()) ? "??????" : "????????????");
                }
                if (!StringUtils.isEmpty(sku.getPrice())) {
                    tempRow.createCell(5).setCellValue(getSkuPriceExportInfo(sku));
                }
                if (Objects.nonNull(spu.getThirdCategory())) {
                    tempRow.createCell(6).setCellValue(spu.getThirdCategory().getName());
                }
                if (Objects.nonNull(spu.getBrand())) {
                    tempRow.createCell(7).setCellValue(spu.getBrand().getName());
                }
                tempRow.createCell(8).setCellValue(sku.getStock());
                if (!StringUtils.isEmpty(spu.getStoreName())) {
                    tempRow.createCell(9).setCellValue(spu.getStoreName());
                } else if (spu.getStoreId() == CommonConstant.ADMIN_STOREID) {
                    tempRow.createCell(9).setCellValue("??????");
                } else {
                    tempRow.createCell(9).setCellValue("");
                }
            });

            int intSkip = Integer.parseInt(skip.toString()) + spu.getSkus().size();
            skip.delete(0, skip.length());
            skip.append(String.valueOf(intSkip));
        });
    }


    /**
     * ?????????????????????????????????
     *
     * @param spuList ????????????
     * @return ????????????
     */
    private List<PmsGoods> fillExportSpuOtherInfo(List<PmsGoods> spuList) {
        spuList.forEach(spu -> {
            if (spu.getStoreId() != CommonConstant.ADMIN_STOREID) {
                TStoreInfo storeInfo = storeInfoService.queryStoreInfo(spu.getStoreId());
                spu.setStoreName(storeInfo == null ? "" : storeInfo.getStoreName());
            }
            spu.setThirdCategory(categoryService.queryCategoryById(spu.getThirdCateId()));
            spu.setBrand(brandService.queryBrandById(spu.getBrandId(), CommonConstant.QUERY_WITH_NO_STORE));
            spu.setSkus(skuService.querySkuBySpuId(spu.getId(), spu.getStoreId(), PmsSkuItem.SPEC, PmsSkuItem.BATCH));
        });
        return spuList;
    }

    /**
     * ??????????????????????????????
     *
     * @param sku ????????????
     * @return ??????????????????
     */
    private String getSkuPriceExportInfo(PmsSku sku) {
        StringBuilder price = new StringBuilder();
        if (sku.isBatchSku()) {
            sku.getSkuBatchList().forEach(skuBatch ->
                    price.append(skuBatch.getBatchInterval()).append("???").append(":").append(String.format("%.2f", skuBatch.getBatchPrice())).append(" | ")
            );
        } else {
            price.append(String.format("%.2f", sku.getPrice())).append("|");
        }
        return price.substring(0, price.lastIndexOf("|"));
    }

}
