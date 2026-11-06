import { $, esc, formatDate } from '../utils/dom.js';
import { api } from '../api/request.js';
import { emptyState, showToast } from './_ui.js';

const statusNames = { DRAFT:'草稿', PENDING_RECEIVE:'待接收', PENDING_DOCUMENT:'待完善文档', PENDING_APPROVAL:'待审批', APPROVED:'已审批待排期', IN_DEVELOPMENT:'开发中', IN_TESTING:'测试中', PENDING_RELEASE:'待发布', COMPLETED:'已完成', REJECTED:'已驳回' };

export async function renderSchedule() {
  const page = $('#page');
  page.innerHTML = `<div class="page-container">
    <div class="page-header"><div><div class="page-title">排期与人员负载</div><div class="page-subtitle">用时间线识别排期冲突，用容量视图平衡团队工作量</div></div>
      <div class="page-actions"><button class="btn btn-secondary" id="schedule-today">回到今天</button><button class="btn btn-primary" data-permission="schedule:edit" id="schedule-optimize">负载建议</button></div></div>
    <div class="schedule-kpis" id="schedule-kpis"></div>
    <div class="schedule-layout"><section class="card schedule-main"><div class="card-header"><div><div class="card-title">需求甘特图</div><div class="card-caption">计划周期与当前进度</div></div><div class="gantt-legend"><span><i class="blue"></i>进行中</span><span><i class="green"></i>已完成</span><span><i class="red"></i>已逾期</span></div></div><div id="gantt-wrap" class="gantt-wrap"><div class="page-loading"><span class="spinner"></span></div></div></section>
      <aside class="card workload-card"><div class="card-header"><div><div class="card-title">人员负载</div><div class="card-caption">按需求预计工时汇总</div></div></div><div id="workload-list" class="workload-list"></div></aside></div>
  </div>`;
  try {
    const res = await api('/requirements?page=0&size=200');
    const items = res?.data?.content || res?.data?.items || [];
    renderKpis(items); renderGantt(items); renderWorkload(items);
  } catch (e) {
    $('#gantt-wrap').innerHTML = emptyState('!', e.message || '排期加载失败');
  }
  $('#schedule-today').onclick = () => document.querySelector('.gantt-today')?.scrollIntoView({ inline:'center', block:'nearest', behavior:'smooth' });
  $('#schedule-optimize')?.addEventListener('click', () => showToast('已根据预计工时生成负载提示，请优先调整红色人员的排期', 'info'));
}

function renderKpis(items) {
  const today = iso(new Date());
  const planned = items.filter(x => x.plannedStartDate && x.plannedEndDate);
  const overdue = planned.filter(x => x.plannedEndDate < today && !['COMPLETED','REJECTED'].includes(x.status));
  const owners = new Set(planned.map(x => x.devOwner).filter(Boolean));
  const noPlan = items.filter(x => x.status === 'APPROVED' || (x.status === 'IN_DEVELOPMENT' && !x.plannedEndDate));
  $('#schedule-kpis').innerHTML = [
    ['计划中需求', planned.length, 'blue', '已有起止日期'], ['逾期风险', overdue.length, overdue.length?'red':'green', '计划结束已过期'],
    ['参与人员', owners.size, 'violet', '当前排期负责人'], ['待排期', noPlan.length, noPlan.length?'amber':'green', '需要补充计划']
  ].map(([label,value,color,hint]) => `<div class="mini-kpi"><span class="mini-kpi-dot ${color}"></span><div><strong>${value}</strong><span>${label}</span><small>${hint}</small></div></div>`).join('');
}

function renderGantt(items) {
  const planned = items.filter(x => x.plannedStartDate && x.plannedEndDate).sort((a,b) => a.plannedStartDate.localeCompare(b.plannedStartDate));
  if (!planned.length) { $('#gantt-wrap').innerHTML = emptyState('⌁','暂无已排期需求'); return; }
  const dates = planned.flatMap(x => [new Date(x.plannedStartDate), new Date(x.plannedEndDate)]);
  const now = new Date(); dates.push(now);
  let start = new Date(Math.min(...dates)); let end = new Date(Math.max(...dates));
  start.setDate(start.getDate() - 2); end.setDate(end.getDate() + 4);
  const days = Math.max(14, Math.ceil((end-start)/86400000)+1); const cell = 34;
  const headers = Array.from({length:days},(_,i)=>{ const d=new Date(start); d.setDate(start.getDate()+i); const today=iso(d)===iso(now); return `<div class="gantt-day ${today?'gantt-today':''}" style="width:${cell}px"><strong>${d.getDate()}</strong><span>${['日','一','二','三','四','五','六'][d.getDay()]}</span></div>`; }).join('');
  const rows = planned.map(r => {
    const s=Math.max(0,Math.round((new Date(r.plannedStartDate)-start)/86400000));
    const len=Math.max(1,Math.round((new Date(r.plannedEndDate)-new Date(r.plannedStartDate))/86400000)+1);
    const overdue=r.plannedEndDate<iso(now)&&!['COMPLETED','REJECTED'].includes(r.status); const cls=r.status==='COMPLETED'?'done':overdue?'overdue':'active';
    return `<div class="gantt-row"><div class="gantt-label"><strong title="${esc(r.title)}">${esc(r.title)}</strong><span>${esc(r.devOwner||'未指派')} · ${statusNames[r.status]||r.status}</span></div><div class="gantt-track" style="width:${days*cell}px;background-size:${cell}px 100%"><div class="gantt-bar ${cls}" style="left:${s*cell+3}px;width:${len*cell-6}px"><span style="width:${r.progress||0}%"></span><b>${r.progress||0}%</b></div></div></div>`;
  }).join('');
  $('#gantt-wrap').innerHTML = `<div class="gantt"><div class="gantt-header"><div class="gantt-label"><strong>${start.getFullYear()} 年 ${start.getMonth()+1} 月</strong></div><div class="gantt-days" style="width:${days*cell}px">${headers}</div></div>${rows}</div>`;
}

function renderWorkload(items) {
  const grouped={}; items.filter(x=>x.devOwner&&!['COMPLETED','REJECTED'].includes(x.status)).forEach(x=>{ const g=grouped[x.devOwner]||(grouped[x.devOwner]={hours:0,count:0,overdue:0}); g.hours+=Number(x.estimatedHours||x.workloadEstimate||0); g.count++; if(x.plannedEndDate&&x.plannedEndDate<iso(new Date()))g.overdue++; });
  const rows=Object.entries(grouped).sort((a,b)=>b[1].hours-a[1].hours);
  $('#workload-list').innerHTML = rows.length ? rows.map(([name,v])=>{ const capacity=80; const pct=Math.min(140,Math.round(v.hours/capacity*100)); const level=pct>100?'danger':pct>80?'warning':'healthy'; return `<div class="workload-item"><div class="workload-person"><span class="avatar avatar-sm">${esc(name[0])}</span><div><strong>${esc(name)}</strong><span>${v.count} 个需求${v.overdue?` · ${v.overdue} 个逾期`:''}</span></div><b class="load-${level}">${pct}%</b></div><div class="load-track"><span class="${level}" style="width:${Math.min(100,pct)}%"></span></div><div class="workload-meta"><span>${v.hours.toFixed(1)}h 已排</span><span>${capacity}h 容量</span></div></div>`; }).join('') : emptyState('♙','暂无负责人负载数据');
}
function iso(d){ return d.toISOString().slice(0,10); }
