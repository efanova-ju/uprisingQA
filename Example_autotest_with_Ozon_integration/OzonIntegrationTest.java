import org.joda.time.DateTime;
import org.junit.Test;
import base.NdsMode;
import base.NdsTypes;
import helpers.DateHelper;
import helpers.StringDateHelper;
import requisites.partnersIntegration.OzonIntegrationModel;
import user.TaxSystem;
import user.UserData;
import rest.model.request.common.NdsPositionType;
import rest.model.request.common.NdsRateType;
import rest.model.request.common.PublicNdsType;
import rest.model.request.marketplaces.MarketplaceRequestDataHelper;
import rest.model.request.requisites.nds.NdsRatePeriodData;
import rest.model.response.data.stock.GoodDataModel;
import rest.service.NdsRatePeriodsRestSteps;
import rest.service.StockRestSteps;
import rest.service.marketplaces.MarketplacesRestSteps;
import steps.bookkeeping.accounting.documents.buy.CommissionAgentReportSteps;
import steps.requisites.partnersIntegrations.OzonSteps;
import tests.MainTest;

import java.io.File;
import java.util.*;

import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

public class OzonIntegrationTest extends MainTest {

    @Steps
    protected MarketplacesRestSteps marketplacesRestSteps;
    @Steps
    protected OzonSteps ozonSteps;
    @Steps
    protected CommissionAgentReportSteps commissionAgentReportSteps;
    @Steps
    protected InternalBillingApiSteps internalBillingApiSteps;
    @Steps
    protected StockRestSteps stockRestSteps;
    @Steps
    protected NdsRatePeriodsRestSteps ndsRatePeriodsRestSteps;

    private static final OzonIntegrationModel ozonKey = new OzonIntegrationModel();


    @Test
    @Category(OzonIntegrationTests.class)
    public void check_import_commission_agent_report_ozon_last_year_with_items_different_nds_rates_by_product_and_acc_politics() {
        OzonIntegrationModel ozonKey =  new OzonIntegrationModel("11111", "1aa11a1a-11a1-1111-a1a1-1a1aaa1a1aaa");
        String filePath = "realizationOzonProductAndAccPoliticsNdsRates2025.json";
        File file = new File(getClass().getResource(filePath).getPath());
        DateTime startDate = new DateTime().withDate(2025, 12, 01);
        DateTime endDate = new DateTime().withDate(2025, 12, 31);
        String startPeriod = StringDateHelper.yyyyMMddHyphen(startDate);
        String endPeriod = StringDateHelper.yyyyMMddHyphen(endDate);
        String reportNumber = "12476977";
        DateTime ndsStartDate = new DateTime().withDate(2025, 1, 1);
        DateTime datePayAndShipment = DateHelper.getDateWithZeroTime(startDate);
        String productName1 = "Энергетический гель Arena с кофином и ВСАА, для бега, Арена";
        String productName2 = "Энергетический гель Арена для бега";

        UserData user = tester.createAccountingUserIpAndFillRequisites(TaxSystem.USN_6);
        String firmId = user.getFirmId();
        String token = oauthApiSteps.getAuthorizationToken(user.getLogin());
        int firmIdInt = user.getFirmIdInt();
        int paymentId = internalBillingApiSteps.createBackofficeBill(BackofficeCreateBillObjectData.getBackofficeCreateBillData(
                firmIdInt, false, "default", null, newBill)).getPaymentHistoryId();
        internalBillingApiSteps.switchOnPayments(paymentId);
        int productNomId = stockRestSteps.getNomenclatureProductId(token);
        ozonSteps.openRequisitesAndActivateOzonIntegrationWithoutSynchronization(firmId, ozonKey);
        stockRestSteps.createProduct(token, GoodDataModel.getProductWithNdsData(productName1, productNomId,
                PublicNdsType.NDS_ZERO.getType(), 145.4, "102492"));
        stockRestSteps.createProduct(token, GoodDataModel.getProductWithPriceNdsAndArticleData(productName2,
                productNomId, 200, NdsPositionType.Top.getIndex(), PublicNdsType.WITHOUT_NDS.getType(), "100455"));
        ndsRatePeriodsRestSteps.setNdsRatePeriodsByDate(token, Arrays.asList(
                NdsRatePeriodData.getNdsPeriodDataByRate(ndsStartDate, ndsStartDate.plusMonths(12).minusDays(1), NdsRateType.Nds5),
                NdsRatePeriodData.getOpenNdsPeriodDataByRate(ndsStartDate.plusMonths(12), NdsRateType.Nds22)));
        marketplacesRestSteps.sendMarketplaceTestData("ozon", firmId, file);
        marketplacesRestSteps.sendMarketplaceRequest(token, MarketplaceRequestDataHelper.getMarketplaceOzonData(firmId,
                startPeriod, endPeriod));

        assertReflectionEquals("Не создан отчет посредника",
                new DocumentInTableModel(reportNumber, StringDateHelper.ddMMyyPoint(endDate), agentName, null, "1"),
                commissionAgentReportSteps.openCommissionAgentReportPageRefreshWaitAndGetReportFromTable(firmId, reportNumber));
        assertReflectionEquals("Данные в таблице по товару с НДС 0% отличаются от ожидаемых",
                new CommissionAgentReportTableModel(productName1, "шт", "1", "2 219,96", NdsTypes.Nds0,
                null, "2219,96", SaleContractorType.Physical, datePayAndShipment, datePayAndShipment),
                commissionAgentReportSteps.openCommissionAgentReportByNumberGetTableItemDataByRow(reportNumber, 1));
        assertReflectionEquals("Данные в таблице по товару с без НДС в карте товара отличаются от ожидаемых",
                new CommissionAgentReportTableModel(productName2, "шт", "1", "2 677,14", NdsTypes.WithoutNds,
                null, "2677,14",  SaleContractorType.Physical, datePayAndShipment, datePayAndShipment),
                commissionAgentReportSteps.getTableItemDataByRow(2));
    }

    @Test
    @Category(OzonIntegrationTests.class)
    public void check_import_commission_agent_report_ozon_groups_items_with_different_sku_in_realization() {
        OzonIntegrationModel ozonKey =  new OzonIntegrationModel("11111", "1aa11a1a-11a1-1111-a1a1-1a1aaa1a1aaa");
        String filePath = "group2SkuOzonProductRealization2SkuReturns.json";
        File file = new File(getClass().getResource(filePath).getPath());
        DateTime startDate = new DateTime().withDate(2025, 9, 1);
        DateTime endDate = new DateTime().withDate(2025, 9, 30);
        String startPeriod = StringDateHelper.yyyyMMddHyphen(startDate);
        String endPeriod = StringDateHelper.yyyyMMddHyphen(endDate);
        String reportNumber = "12827993";
        DateTime ndsStartDate = new DateTime().withDate(2025, 1, 1);
        DateTime datePayAndShipment = DateHelper.getDateWithZeroTime(startDate);
        String productName = "Бретели 1 пара 1 см";

        UserData user = tester.createAccountingUserIpAndFillRequisites(TaxSystem.USN_15);
        String firmId = user.getFirmId();
        String token = oauthApiSteps.getAuthorizationToken(user.getLogin());
        int firmIdInt = user.getFirmIdInt();
        int paymentId = internalBillingApiSteps.createBackofficeBill(BackofficeCreateBillObjectData.getBackofficeCreateBillData(
                firmIdInt, false, "default", null, newBill)).getPaymentHistoryId();
        internalBillingApiSteps.switchOnPayments(paymentId);
        ozonSteps.openRequisitesAndActivateOzonIntegrationWithoutSynchronization(firmId, ozonKey);
        ndsRatePeriodsRestSteps.setNdsRatePeriodsByDate(token, Collections.singletonList(
                NdsRatePeriodData.getOpenNdsPeriodDataByRate(ndsStartDate, NdsRateType.Nds7)));
        marketplacesRestSteps.sendMarketplaceTestData("ozon", firmId, file);
        marketplacesRestSteps.sendMarketplaceRequest(token, MarketplaceRequestDataHelper.getMarketplaceOzonData(firmId,
                startPeriod, endPeriod));

        assertReflectionEquals("Не создан отчет посредника",
                new DocumentInTableModel(reportNumber, StringDateHelper.ddMMyyPoint(endDate), agentName, null, "1"),
                commissionAgentReportSteps.openCommissionAgentReportPageRefreshWaitAndGetReportFromTable(firmId, reportNumber));
        assertReflectionEquals("Данные в таблице реализаций по товару отличаются от ожидаемых",
                new CommissionAgentReportTableModel(productName, "шт", "4", "462,84", NdsTypes.Nds7,
                        "32,40", "495,24",  SaleContractorType.Physical, datePayAndShipment, datePayAndShipment),
                commissionAgentReportSteps.openCommissionAgentReportByNumberGetTableItemDataByRow(reportNumber, 1));
        assertReflectionEquals("Данные в возвратах отличаются от ожидаемых",
                new CommissionAgentReportTableModel(productName, "Текущий отчет", "шт",  "2", "231,42", NdsTypes.Nds7,
                        "16,20", "247,62", SaleContractorType.Physical, datePayAndShipment),
                commissionAgentReportSteps.getTableReturnItemDataByRow(1));
        assertReflectionEquals("Данные в возвратах отличаются от ожидаемых",
                new CommissionAgentReportTableModel(productName, "Текущий отчет", "шт",  "2", "231,42", NdsTypes.Nds7,
                        "16,20", "247,62", SaleContractorType.Physical, datePayAndShipment),
                commissionAgentReportSteps.getTableReturnItemDataByRow(2));
    }
}
