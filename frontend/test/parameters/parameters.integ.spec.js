// Converted from an old Selenium E2E test
import {
    login,
    createTestStore,
} from "__support__/integrated_tests";
import { mount } from "enzyme";

import { startNativeQuestion, saveQuestion, logout } from "../legacy-selenium/support/metabase";
import { LOAD_CURRENT_USER } from "metabase/redux/user";
import { INITIALIZE_SETTINGS, UPDATE_SETTING, updateSetting } from "metabase/admin/settings/settings";
import SettingsSetting from "metabase/admin/settings/components/SettingsSetting";
import SettingToggle from "metabase/admin/settings/components/widgets/SettingToggle";
import Toggle from "metabase/components/Toggle";
import EmbeddingLegalese from "metabase/admin/settings/components/widgets/EmbeddingLegalese";
import { INITIALIZE_QB, SET_QUERY_MODE } from "metabase/query_builder/actions";
import NativeQueryEditor from "metabase/query_builder/components/NativeQueryEditor";
import ColumnarSelector from "metabase/components/ColumnarSelector";
import { delay } from "metabase/lib/promise";
import TagEditorSidebar from "metabase/query_builder/components/template_tags/TagEditorSidebar";
import { getQuery } from "metabase/query_builder/selectors";

async function updateQueryText(store, queryText) {
    // We don't have Ace editor so we have to trigger the Redux action manually
    const newDatasetQuery = getQuery(store.getState())
        .updateQueryText(queryText)
        .datasetQuery()

    return store.dispatch(newDatasetQuery)
}

async function setCategoryParameter(value) {
    // currently just selects the first parameter
    await this.select(":react(Parameters) a").wait().click()
    await this.select(":react(CategoryWidget) li:contains(" + value + ")").wait().click();
    return this;
}

async function checkScalar(value) {
    await this.sleep(250);
    await this.select(".ScalarValue :react(Scalar)").waitText(value);
    return this;
}

const COUNT_ALL = "200";
const COUNT_DOOHICKEY = "56";
const COUNT_GADGET = "43";

describe("parameters", () => {
    beforeAll(async () =>
        await login()
    );

    describe("questions", () => {
        it("should allow users to enable public sharing", async () => {
            const store = await createTestStore();

            // load public sharing settings
            store.pushPath('/admin/settings/public_sharing');
            const app = mount(store.getAppContainer())

            await store.waitForActions([LOAD_CURRENT_USER, INITIALIZE_SETTINGS])

            // // if enabled, disable it so we're in a known state
            // // TODO Atte Keinänen 8/9/17: This should be done with a direct API call in afterAll instead
            const enabledToggleContainer = app.find(SettingToggle).first();

            expect(enabledToggleContainer.text()).toBe("Disabled");

            // toggle it on
            enabledToggleContainer.find(Toggle).simulate("click");
            await store.waitForActions([UPDATE_SETTING])

            // make sure it's enabled
            expect(enabledToggleContainer.text()).toBe("Enabled");
        })

        it("should allow users to enable embedding", async () => {
            const store = await createTestStore();

            // load public sharing settings
            store.pushPath('/admin/settings/embedding_in_other_applications');
            const app = mount(store.getAppContainer())

            await store.waitForActions([LOAD_CURRENT_USER, INITIALIZE_SETTINGS])

            app.find(EmbeddingLegalese).find('button[children="Enable"]').simulate('click');
            await store.waitForActions([UPDATE_SETTING])

            expect(app.find(EmbeddingLegalese).length).toBe(0);
            const enabledToggleContainer = app.find(SettingToggle).first();
            expect(enabledToggleContainer.text()).toBe("Enabled");
        });

        it("should allow users to create parameterized SQL questions", async () => {
            // Don't render Ace editor in tests because it uses many DOM methods that aren't supported by jsdom
            // NOTE Atte Keinänen 8/9/17: Ace provides a MockRenderer class which could be used for pseudo-rendering and
            // testing Ace editor in tests, but it doesn't render stuff to DOM so I'm not sure how practical it would be
            NativeQueryEditor.prototype.loadAceEditor = () => {}

            const store = await createTestStore();

            // load public sharing settings
            store.pushPath('/question');
            const app = mount(store.getAppContainer())
            await store.waitForActions([INITIALIZE_QB]);

            app.find(".Icon-sql").simulate("click");
            await store.waitForActions([SET_QUERY_MODE]);
            store.resetDispatchedActions();

            await updateQueryText(store, "select count(*) from products where {{category}}");
            store.resetDispatchedActions();

            await delay(500);
            console.log(app.find(TagEditorSidebar).debug())
            const fieldFilterVarType = app.find(ColumnarSelector).find(".ColumnarSelector-row[children='Field Filter']");
            fieldFilterVarType.simulate("click");

            // query.updateQueryText(this._editor.getValue()).update(this.props.setDatasetQuery);


            // await d.select(".ColumnarSelector-row:contains(Field)").wait().click();
            // await d.select(".PopoverBody .AdminSelect").wait().sendKeys("cat");
            // await d.select(".ColumnarSelector-row:contains(Category)").wait().click();
            //
            // // test without the parameter
            // await d.select(".RunButton").wait().click();
            // await d::checkScalar(COUNT_ALL);
            //
            // // test the parameter
            // await d::setCategoryParameter("Doohickey");
            // await d.select(".RunButton").wait().click();
            // await d::checkScalar(COUNT_DOOHICKEY);
            //
            // // save the question, required for public link/embedding
            // await d::saveQuestion("sql parameterized");
            //
            // // open sharing panel
            // await d.select(".Icon-share").wait().click();
            //
            // // open application embedding panel
            // await d.select(":react(SharingPane) .text-purple:contains(Embed)").wait().click();
            // // make the parameter editable
            // await d.select(".AdminSelect-content:contains(Disabled)").wait().click();
            // await d.select(":react(Option):contains(Editable)").wait().click();
            // await d.sleep(500);
            // // publish
            // await d.select(".Button:contains(Publish)").wait().click();
            //
            // // get the embed URL
            // const embedUrl = (await d.select(":react(PreviewPane) iframe").wait().attribute("src")).replace(/#.*$/, "");
            //
            // // back to main share panel
            // await d.select("h2 a span:contains(Sharing)").wait().click();
            //
            // // toggle public link on
            // await d.select(":react(SharingPane) :react(Toggle)").wait().click();
            //
            // // get the public URL
            // const publicUrl = (await d.select(":react(CopyWidget) input").wait().attribute("value")).replace(/#.*$/, "");
            //
            // // logout to ensure it works for non-logged in users
            // d::logout();
            //
            // // public url
            // await d.get(publicUrl);
            // await d::checkScalar(COUNT_ALL);
            // await d.sleep(1000); // making sure that the previous api call has finished
            //
            // // manually click parameter
            // await d::setCategoryParameter("Doohickey");
            // await d::checkScalar(COUNT_DOOHICKEY);
            // await d.sleep(1000);
            //
            // // set parameter via url
            // await d.get(publicUrl + "?category=Gadget");
            // await d::checkScalar(COUNT_GADGET);
            // await d.sleep(1000);
            //
            // // embed
            // await d.get(embedUrl);
            // await d::checkScalar(COUNT_ALL);
            // await d.sleep(1000);
            //
            // // manually click parameter
            // await d::setCategoryParameter("Doohickey");
            // await d::checkScalar(COUNT_DOOHICKEY);
            // await d.sleep(1000);
            //
            // // set parameter via url
            // await d.get(embedUrl + "?category=Gadget");
            // await d::checkScalar(COUNT_GADGET);
        });

        afterAll(async () => {
            const store = await createTestStore();

            // Disable public sharing and embedding after running tests
            await store.dispatch(updateSetting({ key: "enable-public-sharing", value: false }))
            await store.dispatch(updateSetting({ key: "enable-embedding", value: false }))
        })
    });
});
