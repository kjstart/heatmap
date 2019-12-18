package com.com.demo.heatmap.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.com.demo.heatmap.entity.PageEvent;
import com.com.demo.heatmap.repo.PageEventRepository;

@RestController
@RequestMapping("/page_event")
public class PageEventController {

	@Autowired
	PageEventRepository repo;

	@RequestMapping("/test")
	public String test() {
		Long time = System.currentTimeMillis();
		return "Test " + time;
	}

	@RequestMapping(value = "/callback", method = RequestMethod.POST)
	public ResponseEntity callback(@RequestBody String payload) throws Exception {
		ForkJoinPool.commonPool().submit(() -> {
			try {
				JSONObject requestBaseJO = new JSONObject(payload);
				if (requestBaseJO.has("events")) {
					JSONArray eventArray = requestBaseJO.getJSONArray("events");
					for (int i = 0; i < eventArray.length(); i++) {
						JSONObject requestJO = eventArray.getJSONObject(i);
						PageEvent pageEvent = new PageEvent();
						pageEvent.setUserId(
								requestJO.has("user_id") && !StringUtils.isEmpty(requestJO.getString("user_id"))
										? requestJO.getString("user_id")
										: null);
						pageEvent.setUrlHash(
								requestJO.has("url_hash") && !StringUtils.isEmpty(requestJO.getString("url_hash"))
										? requestJO.getString("url_hash")
										: null);
						pageEvent.setEventType(
								requestJO.has("event_type") && !StringUtils.isEmpty(requestJO.getString("event_type"))
										? requestJO.getString("event_type")
										: null);
						pageEvent.setPageSection(requestJO.has("page_section")
								&& !StringUtils.isEmpty(requestJO.getString("page_section"))
										? Integer.parseInt(requestJO.getString("page_section"))
										: null);
						pageEvent.setStayTime(
								requestJO.has("stay_time") && !StringUtils.isEmpty(requestJO.getString("stay_time"))
										? Integer.parseInt(requestJO.getString("stay_time"))
										: null);
						pageEvent.setCursorX(
								requestJO.has("cursor_x") && !StringUtils.isEmpty(requestJO.getString("cursor_x"))
										? Float.parseFloat(requestJO.getString("cursor_x"))
										: null);
						pageEvent.setCursorY(
								requestJO.has("cursor_y") && !StringUtils.isEmpty(requestJO.getString("cursor_y"))
										? Float.parseFloat(requestJO.getString("cursor_y"))
										: null);
						if (pageEvent.getEventType() != null) {
							repo.save(pageEvent);
						}
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
		return ResponseEntity.ok().build();
	}

	@RequestMapping(value = "/position/{urlHash}", method = RequestMethod.GET)
	public ResponseEntity<String> queryPosition(@PathVariable("urlHash") String urlHash) throws Exception {

		List<Object[]> result = repo.findStayTimeByUrlHashAndEventTypeGroupByPosition(urlHash);
		List<Integer[]> smoothedResult = smoothPostion(result);
//		smoothedResult = combinePosition(smoothedResult);
		result = normalizePosition(smoothedResult);
//		List<Integer[]> percentageResult = assignColorPoint(result);
		List<Integer[]> percentageResult = buildColorArray(result);
		JSONArray resultJA = new JSONArray();
		for (Integer[] record : percentageResult) {
			JSONObject jo = new JSONObject();
			jo.put("position", record[0]);
			jo.put("color", record[1]);
			resultJA.put(jo);
		}

		HttpHeaders headers = new HttpHeaders();
		headers.set("Access-Control-Allow-Origin", "*");
		headers.set("Access-Control-Allow-Methods", "GET");
		headers.set("Access-Control-Allow-Headers", "x-requested-with,content-type");

		return new ResponseEntity<String>(resultJA.toString(), headers, HttpStatus.OK);
	}

	private List<Integer[]> buildColorArray(List<Object[]> positions) {
		double[] all = new double[100];
		for (Object[] curPo : positions) {
			all[(Integer) curPo[0]] = (Double) curPo[1];
		}

		List<Integer[]> colorArray = new ArrayList<Integer[]>();
		for (int i = 5; i > 0; i--) {
			int peak = findPeak(all);
			if (peak > 0) {
				colorArray.add(new Integer[] { peak, i });
				half10(all, peak);
			}
		}
		Collections.sort(colorArray, new Comparator<Integer[]>() {
			@Override
			public int compare(Integer[] u1, Integer[] u2) {
				return u1[0].compareTo(u2[0]);
			}
		});
		if (colorArray.size() > 0 && colorArray.get(0)[0] > 10) {
			colorArray.add(new Integer[] { 0, 0 });
		}
		if (colorArray.size() > 0 && colorArray.get(colorArray.size() - 1)[0] < 90) {
			colorArray.add(new Integer[] { 100, 0 });
		}

		Collections.sort(colorArray, new Comparator<Integer[]>() {
			@Override
			public int compare(Integer[] u1, Integer[] u2) {
				return u1[0].compareTo(u2[0]);
			}
		});

		List<Integer[]> removedCloser = new ArrayList();
		Integer last = -100;
		for (Integer[] item : colorArray) {
			if (last + 10 < item[0]) {
				removedCloser.add(item);
			}
			last = item[0];
		}
		return removedCloser;
	}

	private void half10(double[] positions, int index) {
		if (index < 5 || index >= 95) {
			throw new IllegalArgumentException("Zero10 index out of range");
		}
		for (int j = -5; j < 5; j++) {
			positions[index + j] = positions[index + j] / 2;
		}
	}

	private int findPeak(double[] positions) {
		double[] avgArray = new double[100];
		int peak = 0;
		for (int i = 5; i < 95; i++) {
			Double sum = 0D;
			for (int j = -5; j < 5; j++) {
				sum += positions[i + j];
			}
			double avg = sum / 10;
			if (avg > 0) {
				avgArray[i] = avg;
				if (avgArray[peak] < sum / 10) {
					peak = i;
				}
			}
		}
		return peak;
	}

//	private List<Integer[]> assignColorPoint(List<Object[]> positionList) {
//		int[] colorArray = new int[10];
//		for (Integer[] curPoint : positionList) {
//			colorArray[curPoint[0]] = curPoint[1];
//		}
//		int firstColor = colorArray[0];
//		int beginPoint = 0;
//		int curColor = colorArray[0];
//		List<Integer[]> finalArray = new ArrayList<Integer[]>();
//		finalArray.add(new Integer[] { 0, firstColor });
//		for (int i = 1; i < colorArray.length; i++) {
//			if (beginPoint == 0 && colorArray[i] != firstColor) {
//				beginPoint = i;
//				curColor = colorArray[i];
//			}
//			if (beginPoint > 0 && curColor != colorArray[i]) {
//				finalArray.add(new Integer[] { beginPoint * 10 + (i - beginPoint) * 5, curColor });
//				curColor = colorArray[i];
//				beginPoint = i;
//			}
//		}
//		finalArray.add(new Integer[] { 100, curColor });
//		return finalArray;
//	}

	private List<Integer[]> combinePosition(List<Integer[]> positionList) {
		List<Integer[]> newList = new ArrayList<Integer[]>();
		for (int i = 0; i < positionList.size() - 4; i += 4) {
			newList.add(new Integer[] { i / 4, positionList.get(i)[1] + positionList.get(i + 1)[1]
					+ positionList.get(i + 2)[1] + positionList.get(i + 3)[1] });
		}
		return newList;
	}

	private List<Object[]> normalizePosition(List<Integer[]> positionList) {
		List<Object[]> result = new ArrayList<Object[]>();
		for (Integer[] curPo : positionList) {
			Double value = 0D;
			if (curPo[1] > 1) {
				value = Math.log(new Double(curPo[1]));
			} else {
				value = new Double(curPo[1]);
			}
			result.add(new Object[] { curPo[0], value });
		}
		return result;
//		Integer max = -1;
//		Integer min = Integer.MAX_VALUE;
//		for (Integer[] curPo : positionList) {
//			if (curPo[1] > max) {
//				max = curPo[1];
//			}
//
//			if (curPo[1] < min) {
//				min = curPo[1];
//			}
//		}
//		for (Integer[] curPo : positionList) {
//			float curTime = curPo[1] + 0.0F;
//			curPo[1] = Math.round(((curTime - min) / max) * 5);
//		}
	}

	private List<Integer[]> smoothPostion(List<Object[]> result) {
		int[] in = new int[100];
		for (Object[] curPosition : result) {
			in[((BigDecimal) curPosition[0]).intValue()] = ((BigDecimal) curPosition[1]).intValue();
		}
		for (int j = 0; j < 3; j++) {
			if (in[0] == 0) {
				in[0] = in[1] / 2;
			}
			if (in[99] == 0) {
				in[99] = in[98] / 2;
			}
			for (int i = 1; i < 99; i++) {
				if (in[i] == 0) {
					in[i] = (in[i - 1] + in[i + 1]) / 2;
				}
			}
		}
		List<Integer[]> newResult = new ArrayList<Integer[]>();
		for (int i = 0; i < 100; i++) {
			if (in[i] != 0) {
				newResult.add(new Integer[] { i, in[i] });
			}
		}
		return newResult;
	}

	@RequestMapping(value = "/click/{urlHash}", method = RequestMethod.GET)
	public ResponseEntity<String> queryPosition(@PathVariable("urlHash") String urlHash,
			@RequestParam("pageWidth") Integer pageWidth, @RequestParam("pageHeight") Integer pageHeight)
			throws Exception {

		List<Object[]> result = repo.findByUrlHashGroupByCursor(urlHash);
		JSONArray dataJA = new JSONArray();
		Integer maxClick = 0;
		for (Object[] curPoint : result) {
			JSONArray curJA = new JSONArray();
			curJA.put(Math.round(pageWidth * (((BigDecimal) curPoint[0]).floatValue() / 100)));
			curJA.put(Math.round(pageHeight * (((BigDecimal) curPoint[1]).floatValue() / 100)));
			Integer pointClick = ((BigDecimal) curPoint[2]).intValue();
			maxClick = maxClick > pointClick ? maxClick : pointClick;
			curJA.put(pointClick);
			dataJA.put(curJA);
		}

		JSONObject resultJO = new JSONObject();
		resultJO.put("data", dataJA);
		resultJO.put("max", maxClick);
		HttpHeaders headers = new HttpHeaders();
		headers.set("Access-Control-Allow-Origin", "*");
		headers.set("Access-Control-Allow-Methods", "GET");
		headers.set("Access-Control-Allow-Headers", "x-requested-with,content-type");

		return new ResponseEntity<String>(resultJO.toString(), headers, HttpStatus.OK);
	}
}
