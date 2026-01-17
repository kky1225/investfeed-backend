package com.example.investfeed.domain.sect.service

import com.example.investfeed.domain.sect.dto.req.SectListReq
import com.example.investfeed.domain.sect.dto.res.SectListItem
import com.example.investfeed.domain.sect.dto.res.SectListRes
import com.example.investfeed.kiwoom.sect.client.SectClient
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectIndexReq
import org.springframework.stereotype.Service

@Service
class SectService(
    private val sectClient: SectClient,
) {

    fun sectIndexList(
        req: SectListReq
    ): SectListRes {
        val kiwoomSectIndexRes = sectClient.sectIndexList(
            KiwoomSectIndexReq(
                inds_cd = req.indsCd,
            )
        )

        val sectList = mutableListOf<SectListItem>()
        if (kiwoomSectIndexRes.return_code == 0) {
            kiwoomSectIndexRes.all_inds_idex?.forEach {
                sectList.add(
                    SectListItem(
                        stkCd = it.stk_cd,
                        stkNm = it.stk_nm,
                        preSig = it.pre_sig,
                        fluRt = it.flu_rt,
                        curPrc = it.cur_prc,
                        trdeQty = it.trde_qty,
                        trdePrica = it.trde_prica,
                    )
                )
            }
        }

        return SectListRes(
            sectList = sectList
        )
    }
}