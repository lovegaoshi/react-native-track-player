//
//  WaveformAudioTap.swift
//  SwiftAudioEx
//
//  Created by Rachel on 8/11/25.
//

import Foundation
import CoreAudio
import Accelerate

struct AudioAnalysis {
    let amplitude: Float        // Overall amplitude (0-1)
    let eqBands: [Float]        // Normalized energy in each frequency band (0-1)
}

public class WaveformAudioTap: AudioTap {
    static var outputs = [String]()
    
    // Frequency ranges for EQ bands (in Hz)
    // HACK: i dont think this works; need to review, but it compiles nonetheless
    private let frequencyRanges = [
        (20, 60),     // Sub bass
        (60, 250),    // Bass
        (250, 500),   // Low mids
        (500, 2000),  // Mids
        (2000, 4000), // Upper mids
        (4000, 20000) // Highs
    ]
    private let fftLength: Int
    
    private let emit: (_ data: Any?) -> Void
    private var fftSetup: vDSP_DFT_Setup?

    let tapIndex: Int

    public init(
        mFFTLength: Int = 1024,
        mEmit: @escaping (_ data: Any) -> Void = { print($0) }
    ){
        self.tapIndex = 0
        self.fftLength = mFFTLength
        self.emit = mEmit
        fftSetup = vDSP_DFT_zop_CreateSetup(
            nil,
            UInt(mFFTLength),
            vDSP_DFT_Direction.FORWARD
        )
    }
    
    deinit {
        if let setup = fftSetup {
            vDSP_DFT_DestroySetup(setup)
        }
    }
    
    public override func initialize() {
        Self.outputs.append("audioTap \(tapIndex): initialize")
    }

    public override func finalize() {
        Self.outputs.append("audioTap \(tapIndex): finalize")
    }

    public override func prepare(description: AudioStreamBasicDescription) {
        Self.outputs.append("audioTap \(tapIndex): prepare")
    }

    public override func unprepare() {
        Self.outputs.append("audioTap \(tapIndex): unprepare")
    }

    public override func process(numberOfFrames: Int, buffer: UnsafeMutableAudioBufferListPointer) {
        Self.outputs.append("audioTap \(tapIndex): process")
        // Get the first buffer (assuming mono/first channel of stereo)
        let firstBuffer = buffer[0]
        guard firstBuffer.mData != nil else {
            self.emit([
                "amplitude": 0,
                "data": Array(repeating: 0, count: frequencyRanges.count)
            ])
            return
        }
        
        // Convert buffer data to array of floats
        let frameLength = Int(firstBuffer.mDataByteSize) / MemoryLayout<Float>.size
        let audioData = UnsafeBufferPointer<Float>(
            start: firstBuffer.mData?.assumingMemoryBound(to: Float.self),
            count: frameLength
        )
        
        // Calculate RMS amplitude
        var rms: Float = 0
        vDSP_rmsqv(audioData.baseAddress!, 1, &rms, UInt(frameLength))
        
        // Prepare for FFT
        var realPart = [Float](repeating: 0, count: fftLength)
        var imagPart = [Float](repeating: 0, count: fftLength)
        
        // Copy audio data to real part, zero-padding if necessary
        let copyLength = min(frameLength, fftLength)
        realPart.withUnsafeMutableBufferPointer { realPtr in
            _ = audioData.prefix(copyLength).enumerated().map { realPtr[$0.0] = $0.1 }
        }
        
        // Apply Hanning window
        var window = [Float](repeating: 0, count: fftLength)
        vDSP_hann_window(&window, UInt(fftLength), Int32(vDSP_HANN_NORM))
        vDSP_vmul(realPart, 1, window, 1, &realPart, 1, UInt(fftLength))
        
        // Perform FFT
        realPart.withUnsafeMutableBufferPointer { realPtr in
            imagPart.withUnsafeMutableBufferPointer { imagPtr in
                var splitComplex = DSPSplitComplex(
                    realp: realPtr.baseAddress!,
                    imagp: imagPtr.baseAddress!
                )
                
                if let fftSetup = fftSetup {
                    vDSP_DFT_Execute(fftSetup,
                                     realPtr.baseAddress!,
                                     imagPtr.baseAddress!,
                                     splitComplex.realp,
                                     splitComplex.imagp)
                }
                
                // Calculate magnitude spectrum
                var magnitudes = [Float](repeating: 0, count: fftLength/2)
                vDSP_zvmags(&splitComplex, 1, &magnitudes, 1, UInt(fftLength/2))
                
                // Calculate frequency resolution
                let sampleRate: Float = 44100 // Adjust based on your audio format
                let frequencyResolution = sampleRate / Float(fftLength)
                
                // Calculate energy in each frequency band
                var eqBands: [Float] = []
                
                for (lowFreq, highFreq) in frequencyRanges {
                    let lowBin = Int(Float(lowFreq) / frequencyResolution)
                    let highBin = min(Int(Float(highFreq) / frequencyResolution), magnitudes.count)
                    
                    let bandMagnitudes = Array(magnitudes[lowBin..<highBin])
                    var bandEnergy: Float = 0
                    vDSP_meanv(bandMagnitudes, 1, &bandEnergy, UInt(bandMagnitudes.count))
                    
                    // Convert to dB scale and normalize
                    let dbValue = 20 * log10(bandEnergy + 1e-6)
                    let normalizedValue = (dbValue + 60) / 60 // Normalize to 0-1 range
                    eqBands.append(max(0, min(1, normalizedValue)))
                }
                
                self.emit([
                    "amplitude": rms,
                    "data": eqBands
                ])
            }
        }
    }
}
