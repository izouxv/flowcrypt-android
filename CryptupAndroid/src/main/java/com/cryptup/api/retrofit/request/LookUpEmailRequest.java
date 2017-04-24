package com.cryptup.api.retrofit.request;

import com.cryptup.api.retrofit.ApiName;
import com.cryptup.api.retrofit.request.model.PostLookUpEmailModel;

/**
 * This class describes the request to the API "https://attester.cryptup.io/lookup/email"
 *
 * @author DenBond7
 *         Date: 24.04.2017
 *         Time: 13:24
 *         E-mail: DenBond7@gmail.com
 */

public class LookUpEmailRequest extends BaseRequest {

    private PostLookUpEmailModel postLookUpEmailModel;

    public LookUpEmailRequest(PostLookUpEmailModel postLookUpEmailModel) {
        super(ApiName.POST_LOOKUP_EMAIL);
        this.postLookUpEmailModel = postLookUpEmailModel;
    }

    public PostLookUpEmailModel getPostLookUpEmailModel() {
        return postLookUpEmailModel;
    }
}
