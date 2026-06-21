insert into maintenance_device_asset
(id, name, device_type, location, status, risk_level, sensor_json, last_inspection_time)
values
('DEV-FAN-01', '一号引风机', '风机', '锅炉房 A 区', '运行', '关注', '["振动","温度","电流","噪声"]', current_timestamp),
('DEV-PUMP-02', '循环水泵二号', '泵', '动力站 B 区', '运行', '预警', '["入口压力","出口压力","振动","温度"]', current_timestamp),
('DEV-MOTOR-03', '输送线主电机', '电机', '产线 3 号位', '待检修', '严重', '["电流","绝缘电阻","温度","外观图片"]', current_timestamp),
('DEV-GEAR-04', '减速齿轮箱', '齿轮箱', '包装线末端', '运行', '正常', '["油温","振动","噪声","油液"]', current_timestamp)
on conflict (id) do nothing;

insert into maintenance_fault_case
(id, device_type, fault_name, symptom_json, image_feature_json, cause, solution, severity)
values
('CASE-001', '风机', '轴承早期磨损', '["温度升高","周期性异响","振动增大"]', '["油污","轴承座发热","轻微磨痕"]', '润滑不足或轴承游隙异常导致滚动体局部磨损', '补充润滑，采集频谱，检查轴承游隙，必要时计划停机更换', 3),
('CASE-002', '泵', '汽蚀与入口堵塞', '["压力波动","流量下降","泵体振动"]', '["入口滤网污堵","管路锈蚀","密封处漏液"]', '入口阻力过大或液位不足导致汽蚀', '检查入口阀门、滤网和液位，排气后复测振动与压力', 4),
('CASE-003', '电机', '绕组过热与绝缘下降', '["外壳高温","电流异常","焦味"]', '["焦痕","变色","绝缘破损"]', '长期过载、散热不良或绝缘老化引起绕组局部过热', '立即停机断电，测绝缘电阻，检查接线端子和散热通道', 5),
('CASE-004', '齿轮箱', '齿面点蚀', '["啮合噪声","油液金属屑","振动边频"]', '["金属屑","齿面麻点","油液浑浊"]', '载荷冲击或润滑油污染导致齿面疲劳', '取样化验油液，检查齿面，过滤或更换润滑油', 3)
on conflict (id) do nothing;

insert into maintenance_task
(id, device_id, title, priority, status, step_json, spare_part_json, acceptance_json, created_at)
values
('TASK-1001', 'DEV-MOTOR-03', '主电机绝缘异常复检', 'P1', '待处理',
 '["断电挂牌上锁","测量三相绝缘电阻","检查接线端子焦痕","清理散热通道","复测空载电流"]',
 '["绝缘表","红外测温仪","接线端子","绝缘胶带"]',
 '["绝缘电阻满足企业标准","空载电流三相平衡","试运行 30 分钟无焦味和异常温升"]',
 current_timestamp)
on conflict (id) do nothing;

insert into maintenance_knowledge_contribution
(id, title, device_type, fault_name, symptom_json, image_feature_json, cause, solution, content, submitter, status, review_note, created_at, reviewed_at, markdown_download_url, pdf_download_url)
values
('KC-1001', '主电机端子过热经验', '电机', '绕组过热与绝缘下降',
 '["外壳高温","电流异常","焦味"]',
 '["焦痕","变色","绝缘破损"]',
 '端子松动、散热不良或绝缘老化会导致局部发热并产生焦味',
 '停机断电后复紧端子，测量绝缘电阻，清理散热通道并做空载试运行',
 '现场经验：端子发黑时不要只更换胶带，应同步检查压接力矩、三相电流平衡和端子排温升。',
 '巡检员张工',
 '已通过',
 '专家复核通过，已纳入案例库',
 current_timestamp,
 current_timestamp,
 '',
 '')
on conflict (id) do nothing;
